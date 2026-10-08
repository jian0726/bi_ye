package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点配置（键值对存库；DB 值覆盖 application.yml 的 site.* 默认值）
 */
@Data
@TableName("t_site_config")
public class SiteConfig {

    /** 配置键，与 application.yml 中 site.* 的 kebab-case 键一致（如 site-name） */
    @TableId
    private String configKey;

    private String configValue;

    private LocalDateTime updateTime;
}
