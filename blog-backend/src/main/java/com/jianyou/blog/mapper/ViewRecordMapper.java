package com.jianyou.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianyou.blog.entity.ViewRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface ViewRecordMapper extends BaseMapper<ViewRecord> {

    /**
     * 记一次浏览：命中 uk_article_visitor 唯一索引时静默跳过（INSERT IGNORE）。
     *
     * @return 1 = 该访客首次浏览该文章（应计入浏览量）；0 = 已浏览过，不计数
     */
    @Insert("""
            INSERT IGNORE INTO t_view_record (article_id, visitor_key, create_time)
            VALUES (#{articleId}, #{visitorKey}, NOW())
            """)
    int insertIgnore(@Param("articleId") Long articleId, @Param("visitorKey") String visitorKey);
}
