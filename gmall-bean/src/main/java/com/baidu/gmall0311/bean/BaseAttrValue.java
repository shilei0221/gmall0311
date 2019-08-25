package com.baidu.gmall0311.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-03 23:23
 */
@Data
public class BaseAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;

    //记录最新的参数
    @Transient
    private String urlParam;
}

