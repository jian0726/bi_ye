package com.jianyou.blog.dto;

import lombok.Data;

/**
 * 登出请求：refreshToken 可选（带上才能撤销对应 refresh；不带时仅前端清本地）
 */
@Data
public class LogoutDTO {

    private String refreshToken;
}
