package com.baidu.gmall0311.manage.mapper;

import com.baidu.gmall0311.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-03 23:33
 */
public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {

    //根据三级分类id得到平台属性 以及平台属性值
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);

    /**
     * 根据平台属性值 id 查询平台属性集合
     * @param valueIds
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}
