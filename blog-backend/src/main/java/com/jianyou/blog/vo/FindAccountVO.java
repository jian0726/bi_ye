package com.jianyou.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 找回密码第一步响应：该账号可用的验证方式（脱敏）
 */
@Data
@Builder
public class FindAccountVO {

    /** 账号是否存在。不存在时不返回任何可用方式，前端统一提示 */
    private boolean exists;

    /** 可用渠道列表；只有一个时前端可跳过选择步骤 */
    private List<Channel> channels;

    /** 单个可用渠道 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        /** phone / email */
        private String channel;
        /** 脱敏后的接收目标，如 138****0001 / ji***@local.dev */
        private String masked;
    }
}
