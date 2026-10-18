package com.greatprice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greatprice.entity.PriceHistory;
import com.greatprice.entity.PriceHistoryRow;
import org.apache.ibatis.annotations.Param;

public interface PriceHistoryMapper extends BaseMapper<PriceHistory> {

    IPage<PriceHistoryRow> selectAdminPage(IPage<PriceHistoryRow> page,
                                           @Param("productId") Long productId,
                                           @Param("dealId") Long dealId);

    PriceHistoryRow selectAdminById(@Param("id") Long id);
}
