package com.greatprice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greatprice.entity.Product;
import com.greatprice.entity.ProductRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

    IPage<ProductRow> selectAdminPage(IPage<ProductRow> page,
                                      @Param("categoryId") Long categoryId,
                                      @Param("status") String status,
                                      @Param("keyword") String keyword);

    @Update("UPDATE product SET status = 'INACTIVE' WHERE id = #{id} AND deleted = 0 AND status = 'ACTIVE'")
    int disable(@Param("id") Long id);

    @Update("UPDATE product SET status = 'ACTIVE' WHERE id = #{id} AND deleted = 0 AND status = 'INACTIVE'")
    int enable(@Param("id") Long id);

    @Update("UPDATE product SET deleted = 1, status = 'INACTIVE' WHERE id = #{id} AND deleted = 0")
    int markDeleted(@Param("id") Long id);
}
