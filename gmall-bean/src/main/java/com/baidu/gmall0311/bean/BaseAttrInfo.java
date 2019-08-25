package com.baidu.gmall0311.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-03 23:23
 */
@Data
public class BaseAttrInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY) //获取自增的主键 id
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    //添加平台属性值对象集合
    @Transient  //该注解表示该字段不是数据库字段，但是是业务需要的
    private List<BaseAttrValue> attrValueList = new ArrayList<>();

}