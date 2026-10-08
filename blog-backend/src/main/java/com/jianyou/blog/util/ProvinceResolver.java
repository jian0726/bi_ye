package com.jianyou.blog.util;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * IP 归属地（省份）解析 —— 基于 ip2region 离线 xdb 数据库。
 *
 * <p>数据文件 {@code resources/ip2region/ip2region_v4.xdb} 在启动时整体载入内存，
 * 查询为纯内存二分查找（微秒级），不产生任何网络请求；xdb 文件缺失或加载失败时
 * 降级为「未知」，不影响主流程。</p>
 *
 * <p>只覆盖 IPv4：传 IPv6 地址时直接返回「未知」（未引入 37MB 的 v6 数据文件）。</p>
 */
@Slf4j
@Component
public class ProvinceResolver {

    private static final String XDB_PATH = "ip2region/ip2region_v4.xdb";

    /** 公网 IP：全内存 searcher，只读查询线程安全；加载失败时为 null（降级「未知」） */
    private final Searcher searcher;

    public ProvinceResolver() {
        Searcher loaded = null;
        try (InputStream in = new ClassPathResource(XDB_PATH).getInputStream()) {
            LongByteArray buffer = Searcher.loadContentFromInputStream(in);
            loaded = Searcher.newWithBuffer(Version.IPv4, buffer);
            log.info("ip2region xdb 载入成功: {} ({} bytes)", XDB_PATH, buffer.length());
        } catch (Exception e) {
            // 数据文件缺失/损坏时不阻断启动，仅退化为「未知」
            log.warn("ip2region xdb 载入失败，IP 归属地将统一返回「未知」: {}", e.getMessage());
        }
        this.searcher = loaded;
    }

    /**
     * 解析 IP 归属省份。
     *
     * @return 「本地」（回环/内网）、省份名（已去掉「省/市/自治区」后缀）、或「未知」
     */
    public String resolve(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }
        if (isLocal(ip)) {
            return "本地";
        }
        if (searcher == null) {
            return "未知";
        }
        try {
            // 返回格式（v3 数据）：国家|省份|城市|ISP|国家码，例：中国|广东省|深圳市|电信|CN
            String region = searcher.search(ip);
            if (!StringUtils.hasText(region)) {
                return "未知";
            }
            String[] parts = region.split("\\|");
            if (parts.length < 2) {
                return "未知";
            }
            String country = parts[0];
            String province = parts[1];
            // 私网段在 xdb 中标记为 Reserved，与 isLocal 互为兜底
            if (!StringUtils.hasText(province) || "0".equals(province)
                    || "Reserved".equalsIgnoreCase(country) || "Reserved".equalsIgnoreCase(province)) {
                return "本地";
            }
            // 境外 IP：省份位是城市/州名，统一展示国家名，避免出现「California」这类非省份值
            if (!"中国".equals(country)) {
                return country;
            }
            return trimSuffix(province);
        } catch (Exception e) {
            // IPv6 等无法解析的地址落到这里
            log.debug("IP 归属地解析失败: {}", ip);
            return "未知";
        }
    }

    /** 去掉「省 / 市 / 自治区」后缀，与前端展示口径一致（如「广东省」→「广东」） */
    private String trimSuffix(String province) {
        return province
                .replace("维吾尔自治区", "")
                .replace("壮族自治区", "")
                .replace("回族自治区", "")
                .replace("自治区", "")
                .replace("省", "")
                .replace("市", "");
    }

    /** 回环 / 内网地址判断（RFC1918 + 链路本地） */
    private boolean isLocal(String ip) {
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("169.254.")) {
            return true;
        }
        // 172.16.0.0 ~ 172.31.255.255 为私网，其余 172.* 是公网
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
