package com.media.interceptor;

import cn.hutool.json.JSONUtil;
import com.media.dto.Result;
import com.media.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component
public class LoginInterceptor  implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //注意这里如果用response 返回,如果是字符串一定要指定字符串的编码
        //下面是指定返回的json
        response.setContentType("text/html;charset=utf-8");
        response.setContentType("application/json");
        //從綫程中找，沒找到説明沒登錄，返回false
        //登錄攔截直接找綫程中拿，沒有返回false
        if(UserHolder.getUser() ==null) {
            PrintWriter writer = response.getWriter();
            writer.println(JSONUtil.parse(Result.fail("请先登录哦,亲")));
            writer.flush();
            response.setStatus(405);
            return false;
        }
        //BeanUtil.fillBeanWithMap 是cn.hutool.core.bean.BeanUtil 这个包下的方法
        return true;

    }
}
