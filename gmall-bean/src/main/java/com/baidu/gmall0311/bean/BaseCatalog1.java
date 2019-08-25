package com.baidu.gmall0311.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-03 23:21
 */
@Data
public class BaseCatalog1 implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column
    private String id;
    @Column
    private String name;
}