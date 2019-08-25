package com.baidu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.*;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-03 23:04
 */
@Controller
@CrossOrigin
public class ManageController {

    //因为使用了dubbo所有不使用Spring中的自动注入注解了
    @Reference
    private ManageService manageService;

    @RequestMapping("index")
    public String index() {

        return "index";
    }

    /**
     * 查询所有的一级分类
     * @return
     */
    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1() {

        //调用业务层
        return manageService.getCatalog1();
    }

    /**
     * 根据一级分类 id 查询二级分类数据
     * @param catalog1Id
     * @return
     */
    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        return manageService.getCatalog2(catalog1Id);
    }

    /**
     * 根据二级分类 id 查询所有的三级分类数据
     * @param catalog2Id
     * @return
     */
    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    /**
     * 根据三级分类id 查询所有的平台属性
     *
     * http://localhost:8082/attrInfoList
     *
     * @param catalog3Id
     * @return
     */
    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }

    /**
     * 在控制器中 如何得到前台页面传递过来的数据
     * 将页面数据库保存到数据库
     *
     *  前台数据传递的时候 是以 json 形式传递 后台控制器接收数据要将json转成对象 @RequestBody
     *
     *
     * 保存需要处理的请求 保存与修改 都需要经过该控制器处理
     *
     * http://localhost:8082/saveAttrInfo
     * @param baseAttrInfo
     * @return
     */
    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
        return "ok";
    }

    /**
     * 根据 attrId 获取属性值集合数据
     *
     * http://localhost:8082/getAttrValueList
     *
     * @param attrId
     * @return
     */
    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        //根据 attrId 获取属性值集合数据

        //这种是为了完成功能而完成功能
//        List<BaseAttrValue> baseAttrValueList = manageService.getAttrValueList(attrId);
//
//        return baseAttrValueList;

        //按照业务需求来说  涉及到属性与属性值  是一对多的关系 所有按业务来讲 应该先判断一下属性是否存在 如果不存在就没必要显示属性值
        //所有应该先查属性是否存在
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);

        return baseAttrInfo.getAttrValueList();
    }

}
