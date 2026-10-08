package com.jianyou.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表留言请求
 */
@Data
public class MessagePostDTO {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 300, message = "留言内容最多 300 字")
    private String content;

    /** 游客昵称（可不填，默认「访客」） */
    @Size(max = 20, message = "昵称最多 20 字")
    private String nickname;

    /** 游客邮箱（仅存档，不回显） */
    @Email(message = "邮箱格式不正确")
    private String email;
}
