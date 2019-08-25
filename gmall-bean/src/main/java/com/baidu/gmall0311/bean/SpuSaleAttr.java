package com.baidu.gmall0311.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-05 22:44
 *
 * 销售属性表
 */
@Data
public class SpuSaleAttr  implements Serializable {

    @Id
    @Column
    String id ;

    @Column
    String spuId;

    @Column
    String saleAttrId;

    @Column
    String saleAttrName;


    //销售属性集合
    @Transient
    List<SpuSaleAttrValue> spuSaleAttrValueList;
}
