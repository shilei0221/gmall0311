package com.baidu.gmall0311.user.controller;

import com.baidu.gmall0311.bean.UserAddress;
import com.baidu.gmall0311.bean.UserInfo;
import com.baidu.gmall0311.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-02 10:29
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;



    @RequestMapping("findAll")
    @ResponseBody
    public List<UserInfo> findAll() {
        return userService.getUserInfoListAll();
    }


    @RequestMapping("getAll")
    @ResponseBody
    public List<UserAddress> getAll(String userId) {
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);

        return userAddressList;
    }

}
