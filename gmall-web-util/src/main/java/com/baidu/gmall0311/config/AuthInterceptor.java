package com.baidu.gmall0311.config;

import com.alibaba.fastjson.JSON;
import com.baidu.gmall0311.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-17 20:25
 *
 * 定义拦截器功能
 *
 *  拦截 token 获取用户昵称
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    //进入控制器之前 页面渲染之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //登录的时候才会产生 token 如果访问商品详情 item.gmall.com/43.html
        //http://passport.gmall.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IumYv-ejiiIsInVzZXJJZCI6IjQifQ.9Nq73lNM1j7cUH6ryRBIssaSYpfBblQQsgM_RfOTPJs&currentIp=192.168.199.1
        String newToken = request.getParameter("newToken");

        if (newToken != null) {
            //将 token 放入 cookie中
            //cookie cookie = new cookie("newToken",newToken)
            CookieUtil.setCookie(request,response,"token",newToken,WebConst.COOKIE_MAXAGE,false);

        }

        //如果说登录之后 用户继续访问 商品详情 此时是否有token  有 在cookie中
        if (newToken == null) {
            newToken = CookieUtil.getCookieValue(request,"token",false);
        }

        //获取到了token 解密 token 获取用户昵称 在页面显示
        if (newToken != null) {
            //解密token 获取用户昵称
            //使用 base64 编码解密  也可以使用 jwt 进行解密
            Map map = getUserMapByToken(newToken);

           String nickName = (String) map.get("nickName");

           //将用户昵称保存到作用域
            request.setAttribute("nickName",nickName);
        }

        //获取方法上的注解 @LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        //获取到方法上面的注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);

        //判断
        if (methodAnnotation != null) {
            //有注解 直接进行认证 调用 verify 控制器
            //因为该控制器 需要传入 token 和 盐 currentIp 所以获取 currentIp
            String currentIp = request.getHeader("X-forwarded-for");

            //http://passport.gmall.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IumYv-ejiiIsInVzZXJJZCI6IjQifQ.9Nq73lNM1j7cUH6ryRBIssaSYpfBblQQsgM_RfOTPJs&currentIp=192.168.199.1
//          使用远程调用 WebConst.VERIFY_ADDRESS
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + newToken + "&currentIp=" + currentIp);

            //判断该控制器返回的值是否成功
            if ("success".equals(result)) {
                //认证成功 说明用户已经登录
                //保存用户 id
                Map map = getUserMapByToken(newToken);

                String userId = (String) map.get("userId");

                //将用户id先保存到作用域
                request.setAttribute("userId",userId);

                return true;
            } else {
                //认证失败
                if (methodAnnotation.autoRedirect()) {
                    //必须登录 跳转登录页面
                    //获取当前请求的url
                    String requestURL  = request.getRequestURL().toString();
                    System.out.println(requestURL + "   ：没有编码之前");

                    //将获取到的url 进行编码
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println(encodeURL + "    :编码后的url");

                    //页面跳转
                    response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeURL);

                    return false;
                }
            }
        }

        return true;
    }


    //解密token 方法
    private Map getUserMapByToken(String newToken) {
        //eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IumYv-ejiiIsInVzZXJJZCI6IjQifQ.9Nq73lNM1j7cUH6ryRBIssaSYpfBblQQsgM_RfOTPJs
        //只需要取出私有部分字符串对其进行base64 解码即可
        String token = StringUtils.substringBetween(newToken, ".");

        //创建对象
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();

        //调用方法进行解密
        byte[] decode = base64UrlCodec.decode(token);

        //将字节数组变成字符串
        String tokenJson = null;

        try {

            tokenJson = new String(decode,"utf-8");

        } catch (Exception e) {
            e.printStackTrace();
        }

        //将字符串转换为 map
        Map map = JSON.parseObject(tokenJson, Map.class);

        return map;

    }

    //进入控制器之后  页面渲染之后 视图解析之前
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    //视图解析完成
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
