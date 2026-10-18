package com.greatprice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greatprice.entity.DealReview;
import com.greatprice.entity.DealReviewRow;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DealReviewMapper extends BaseMapper<DealReview> {

    @Select("""
            SELECT r.id, r.deal_id, r.action, r.reviewer_id, u.display_name AS reviewer_name,
                   r.reason, r.created_at
            FROM deal_review r
            JOIN admin_user u ON u.id = r.reviewer_id
            WHERE r.deal_id = #{dealId}
            ORDER BY r.id DESC
            """)
    List<DealReviewRow> selectByDealId(Long dealId);
}
