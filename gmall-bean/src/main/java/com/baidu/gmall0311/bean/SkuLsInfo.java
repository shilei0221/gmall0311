package com.baidu.gmall0311.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-12 23:00
 *
 * 把es中所有的字段封装到skuLsInfo中。
 */
@Data
public class SkuLsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    //skuId
    String id;

    BigDecimal price;

    String skuName;

    String catalog3Id;

    String skuDefaultImg;

    //热度排名
    Long hotScore=0L;

    List<SkuLsAttrValue> skuAttrValueList;
}
