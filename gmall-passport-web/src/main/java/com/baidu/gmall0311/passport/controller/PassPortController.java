package com.baidu.gmall0311.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall0311.bean.UserInfo;
import com.baidu.gmall0311.passport.util.JwtUtil;
import com.baidu.gmall0311.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-16 22:40
 */
@Controller
public class PassPortController {

    @Value("${token.key}") //定义 头信息
    private String key;

    @Reference
    private UserService userService;

    /**
     * 根据请求跳往登录首页
     * @param request
     * @return
     */
    @RequestMapping("index")
    public String index(HttpServletRequest request) {
        //value 从 url 直接获取
        String originUrl = request.getParameter("originUrl");

        request.setAttribute("originUrl",originUrl);

        return "index";
    }


    /**
     * 调用接口实现登录功能
     * @param userInfo
     * @return
     */
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {

        //获取服务器的IP地址   构建盐  哈希签名部分
        String salt = request.getHeader("X-forwarded-for");

        UserInfo login = userService.login(userInfo);

        if (login != null) {

            //登录成功之后返回 token
            //创建用户信息  私有部分
            Map<String,Object> map = new HashMap<>();
            map.put("userId",login.getId());
            map.put("nickName",login.getNickName());

            //调用 jwt 中的生成token 的方法 生成token
            String token = JwtUtil.encode(key, map, salt);

            return token;
        } else {
            return "fail";
        }
    }

    //http://passport.gmall.com/verify?token=xxx&currentIp=xxx
    //只需要通过用户的Id去 redis 中查询以下是否有用户数据即可
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {

        //获取路径中的 token 值 与盐 ip 值
        String token = request.getParameter("token");
        String salt = request.getParameter("currentIp");

        //调用 jwt 工具类 进行解密 获取用户信息
        Map<String, Object> map = JwtUtil.decode(token, key, salt);

        //判断用户信息是否为空 进行取值
        if (map != null && map.size() > 0) {

            String userId = (String) map.get("userId");

            //调用服务层方法验证redis 中是否有用户信息
            UserInfo userInfo = userService.verify(userId);

            //如果用户对象不为空 返回成功
            if (userInfo != null ) {
                return "success";
            }
        }
        return  "fail";

    }
}
