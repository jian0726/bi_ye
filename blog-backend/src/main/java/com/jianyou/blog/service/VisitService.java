package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jianyou.blog.entity.VisitLog;
import com.jianyou.blog.mapper.VisitLogMapper;
import com.jianyou.blog.util.ProvinceResolver;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 访问埋点服务：异步落库不阻塞请求 + 仪表盘今日访客统计。
 *
 * <p>统计口径（2026-10-06 定稿，对标生产化）：
 * 「今日访问」= 当日<strong>独立访客数</strong>（COUNT(DISTINCT visitor_key)），
 * 「今日访问省份统计」= 同一批行按省份分组后的独立访客数，两者之和一致。
 * visitor_key 复用浏览量的访客标识体系（登录 u:{userId} / 游客 ip:{IP}）。</p>
 */
@Slf4j
@Service
public class VisitService {

    /** 埋点队列容量：有界，满了丢弃最旧数据（埋点可有损，绝不阻塞业务请求） */
    private static final int QUEUE_CAPACITY = 2000;

    /** 队列满导致的丢弃计数（可见性：purge 定时任务周期性输出） */
    private final AtomicLong droppedCount = new AtomicLong();

    /** 单线程异步落库，埋点失败只记日志不影响业务 */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread thread = new Thread(r, "visit-log");
                thread.setDaemon(true);
                return thread;
            },
            // 队列满时静默丢弃并计数（丢弃动作发生在调用线程，绝不能阻塞或抛异常）
            (r, pool) -> droppedCount.incrementAndGet());

    private final VisitLogMapper visitLogMapper;
    private final ProvinceResolver provinceResolver;

    public VisitService(VisitLogMapper visitLogMapper, ProvinceResolver provinceResolver) {
        this.visitLogMapper = visitLogMapper;
        this.provinceResolver = provinceResolver;
    }

    /**
     * 埋点入口（拦截器调用）。省份与访客标识在此同步解析（均为内存操作，微秒级），
     * 仅 INSERT 放入异步队列。
     */
    public void recordAsync(String visitorKey, String ip, String path, String userAgent) {
        executor.execute(() -> {
            try {
                VisitLog visitLog = new VisitLog();
                visitLog.setVisitorKey(truncate(visitorKey, 64));
                visitLog.setIp(truncate(ip, 45));
                visitLog.setProvince(provinceResolver.resolve(ip));
                visitLog.setPath(truncate(path, 200));
                visitLog.setUserAgent(truncate(userAgent, 300));
                visitLog.setCreateTime(LocalDateTime.now());
                visitLogMapper.insert(visitLog);
            } catch (Exception e) {
                log.warn("访问埋点落库失败: {} {}", ip, path, e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    /** 删除 N 天前的访问日志（分批执行防长事务锁表），返回删除总行数 */
    public int purgeBefore(LocalDateTime before) {
        int total = 0;
        int batch;
        do {
            batch = visitLogMapper.delete(new QueryWrapper<VisitLog>()
                    .lt("create_time", before)
                    .last("LIMIT 5000"));
            total += batch;
        } while (batch >= 5000);
        return total;
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }

    /**
     * 今日独立访客数 + 按省份分组的独立访客数（仪表盘使用）。
     * 两者同源同口径：省份各数之和 = 今日访客总数。
     */
    public Map<String, Object> todayStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<Map<String, Object>> totalRows = visitLogMapper.selectMaps(new QueryWrapper<VisitLog>()
                .select("COUNT(DISTINCT visitor_key) AS total")
                .ge("create_time", todayStart));
        long todayVisitTotal = totalRows.isEmpty() || totalRows.get(0).get("total") == null
                ? 0L : Long.parseLong(String.valueOf(totalRows.get(0).get("total")));

        // SELECT province, COUNT(DISTINCT visitor_key) AS count
        //   FROM t_visit_log WHERE create_time >= ? GROUP BY province ORDER BY count DESC
        List<Map<String, Object>> rows = visitLogMapper.selectMaps(new QueryWrapper<VisitLog>()
                .select("province", "COUNT(DISTINCT visitor_key) AS count")
                .ge("create_time", todayStart)
                .groupBy("province")
                .orderByDesc("count"));

        List<Map<String, Object>> provinceStats = rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("province", row.getOrDefault("province", "未知"));
                    item.put("count", Long.parseLong(String.valueOf(row.getOrDefault("count", 0))));
                    return item;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayVisitTotal", todayVisitTotal);
        data.put("provinceStats", provinceStats);
        return data;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
