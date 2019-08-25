package com.baidu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.BaseSaleAttr;
import com.baidu.gmall0311.bean.SpuInfo;
import com.baidu.gmall0311.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-04 12:30
 */
@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    //http://localhost:8082/spuList?catalog3Id=1

    /**
     * 根据 三级分类查询 spu 列表
     * @param catalog3Id
     * @return
     */
//    @RequestMapping("spuList")
//    public List<SpuInfo> spuList(String catalog3Id){
//        return  manageService.spuList(catalog3Id);
//    }
    /**
     * 根据 三级分类查询 spu 列表
     * @param spuInfo
     * @return
     */
    @RequestMapping("spuList")
    public List<SpuInfo> spuList(SpuInfo spuInfo){
        return  manageService.spuList(spuInfo);
    }

    //http://localhost:8082/baseSaleAttrList  销售属性路径
    /**
     * 查询销售属性
     *
     * http://localhost:8082/baseSaleAttrList
     *
     * @return
     */
    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return   manageService.getBaseSaleAttrList();
    }

    /**
     * http://localhost:8082/saveSpuInfo
     *
     * 添加销售属性
     *
     * 如果页面传递的数据是以json 个数传递的 则后台接收的时候 需要使用一个 @RequestBody 注解将其转换为对象
     */
    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        //保存方法
        manageService.saveSpuInfo(spuInfo);
    }

    /**
     * http://localhost:8082/spuImageList?spuId=58
     */
}
