package com.baidu.gmall0311.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-13 23:42
 *
 * 返回结果的类
 */
@Data
public class SkuLsResult implements Serializable {

    //所有的商品
    List<SkuLsInfo> skuLsInfoList;

    //总记录数
    long total;

    //总页数
    long totalPages;

    //平台属性值id集合
    List<String> attrValueIdList;
}

