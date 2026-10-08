package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 友链申请请求（落库待审，博主审核后才上架）
 */
@Data
public class LinkApplyDTO {

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 30, message = "站点名称最多 30 字")
    private String name;

    @NotBlank(message = "站点地址不能为空")
    @Size(max = 200, message = "站点地址过长")
    private String url;

    @Size(max = 300, message = "图标地址过长")
    private String logo;

    @Size(max = 60, message = "站点描述最多 60 字")
    private String description;

    @Size(max = 50, message = "邮箱过长")
    private String email;
}
