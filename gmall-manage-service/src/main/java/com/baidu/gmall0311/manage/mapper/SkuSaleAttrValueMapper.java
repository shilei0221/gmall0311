package com.baidu.gmall0311.manage.mapper;

import com.baidu.gmall0311.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-07 23:13
 */
public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    /**
     * 根据spuId 查询与 skuId 相关的销售属性值集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
