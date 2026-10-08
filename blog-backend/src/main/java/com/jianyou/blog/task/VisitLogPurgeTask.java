package com.jianyou.blog.task;

import com.jianyou.blog.service.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 访问日志定期清理：删除超过保留期的埋点记录，防止 t_visit_log 无限增长。
 *
 * <p>保留期与执行时间均可配置（application.yml 的 app.visit-log.*），
 * 生产部署时通过环境变量覆盖（VISIT_LOG_RETENTION_DAYS / VISIT_LOG_PURGE_CRON）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitLogPurgeTask {

    private final VisitService visitService;

    @Value("${app.visit-log.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${app.visit-log.purge-cron:0 0 4 * * ?}",
            zone = "Asia/Shanghai")
    public void purge() {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int deleted = visitService.purgeBefore(before);
        long dropped = visitService.getDroppedCount();
        log.info("访问日志清理完成：删除 {} 天前记录 {} 行；埋点队列累计丢弃 {} 条",
                retentionDays, deleted, dropped);
    }
}
