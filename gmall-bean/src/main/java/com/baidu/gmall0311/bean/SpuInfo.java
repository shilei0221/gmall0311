package com.baidu.gmall0311.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-04 12:06
 */
@Data
public class SpuInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //获取主键自增的id
    private String id;

    @Column
    private String spuName;

    @Column
    private String description;

    @Column
    private  String catalog3Id;

    //新增图片集合
    //销售属性  销售属性值集合
    @Transient  //该注解表示数据库没有的字段 我们业务需要 所有使用该注解标识
    private List<SpuSaleAttr> spuSaleAttrList;
    @Transient
    private List<SpuImage> spuImageList;

}
