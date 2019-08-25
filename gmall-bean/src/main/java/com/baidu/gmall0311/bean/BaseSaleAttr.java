package com.baidu.gmall0311.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-05 22:17
 */
@Data
public class BaseSaleAttr implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column
    String id ;

    @Column
    String name;
}
