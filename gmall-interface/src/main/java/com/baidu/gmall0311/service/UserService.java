package com.baidu.gmall0311.service;

import com.baidu.gmall0311.bean.UserAddress;
import com.baidu.gmall0311.bean.UserInfo;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-02 10:24
 */
public interface UserService {
    List<UserInfo> getUserInfoListAll();

    void addUser(UserInfo admin);

    void updateUser(UserInfo admin);

    void updateUserByName(String name, UserInfo admin);

    void delUser(UserInfo admin);

    /**
     * 根据 userId查询用户地址列表
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressList(String userId);

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 根据 userId 去查询 redis 中的用户信息
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
