package com.greatprice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greatprice.entity.AdminDealRow;
import com.greatprice.entity.Deal;
import com.greatprice.entity.DealRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 公开查询只返回：已发布、已到发布时间、在促销有效期内、关联商品正常且未删除的好价。SQL 见 resources/mapper/DealMapper.xml。
 */
public interface DealMapper extends BaseMapper<Deal> {

    /**
     * @param keyword 已转义 LIKE 通配符（转义符为 !）的关键词，为 null 时不过滤
     * @param sortBy  排序方式，取值见 {@code DealSortBy}，由 XML 映射为固定的 ORDER BY，不会拼接到 SQL 中
     */
    IPage<DealRow> selectPublicPage(IPage<DealRow> page,
                                    @Param("categoryId") Long categoryId,
                                    @Param("keyword") String keyword,
                                    @Param("featured") Boolean featured,
                                    @Param("sortBy") String sortBy,
                                    @Param("now") LocalDateTime now);

    DealRow selectPublicById(@Param("id") Long id, @Param("now") LocalDateTime now);

    IPage<AdminDealRow> selectAdminPage(IPage<AdminDealRow> page,
                                        @Param("categoryId") Long categoryId,
                                        @Param("productId") Long productId,
                                        @Param("keyword") String keyword,
                                        @Param("dealId") Long dealId,
                                        @Param("status") String status,
                                        @Param("sourceType") String sourceType,
                                        @Param("sortBy") String sortBy);

    AdminDealRow selectAdminById(@Param("id") Long id);

    int updateEditable(Deal deal);

    int revisePublished(Deal deal);

    int submitReview(@Param("id") Long id, @Param("version") Integer version, @Param("actorId") Long actorId);

    int approve(@Param("id") Long id, @Param("version") Integer version,
                @Param("actorId") Long actorId, @Param("now") LocalDateTime now);

    int reject(@Param("id") Long id, @Param("version") Integer version, @Param("actorId") Long actorId,
               @Param("reason") String reason, @Param("now") LocalDateTime now);

    int offline(@Param("id") Long id, @Param("version") Integer version, @Param("reason") String reason);

    int reopen(@Param("id") Long id, @Param("version") Integer version);

    int markVerified(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Select("""
            SELECT COUNT(*) FROM deal
            WHERE status = 'PUBLISHED' AND (expires_at IS NULL OR expires_at > #{now})
            """)
    long countPublishedActive(@Param("now") LocalDateTime now);

    @Select("""
            SELECT COUNT(*) FROM deal
            WHERE status = 'EXPIRED'
               OR (status = 'PUBLISHED' AND expires_at IS NOT NULL AND expires_at <= #{now})
            """)
    long countExpired(@Param("now") LocalDateTime now);
}
