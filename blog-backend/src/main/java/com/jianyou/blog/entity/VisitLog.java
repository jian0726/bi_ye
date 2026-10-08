package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志埋点（门户访问记录，仪表盘「今日访问省份统计」数据源）
 */
@Data
@TableName("t_visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户端 IP */
    private String ip;

    /** 访客标识：登录 u:{userId} / 游客 ip:{IP}，独立访客去重键（与 t_view_record 同体系） */
    private String visitorKey;

    /** 省份（IP 归属地解析结果；本机/内网为「本地」，解析失败为「未知」） */
    private String province;

    /** 访问路径 */
    private String path;

    /** 浏览器 User-Agent */
    private String userAgent;

    private LocalDateTime createTime;
}
