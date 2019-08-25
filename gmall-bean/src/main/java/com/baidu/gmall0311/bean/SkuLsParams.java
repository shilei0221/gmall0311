package com.baidu.gmall0311.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-13 23:41
 *
 * 制作传入参数的类
 */
@Data
public class SkuLsParams implements Serializable {

    private static final long serialVersionUID = 1L;

    //skuName = keyword
    String  keyword;

    String catalog3Id;

    String[] valueId;

    int pageNo=1;

    int pageSize=20;
}
