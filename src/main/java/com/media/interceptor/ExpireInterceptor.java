package com.media.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.media.dto.UserDTO;
import com.media.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.media.utils.RedisConstants.LOGIN_USER_KEY;

@Component
public class ExpireInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token=request.getHeader("authorization");
        if(StringUtils.isBlank(token) || token==null){
            return true;
        }
        String key=LOGIN_USER_KEY+token;
        Map<Object, Object> userDtoMap = stringRedisTemplate.opsForHash().entries(key);
        //判斷是否為空,因爲有可能發過來的token是有的,但是沒有值
        if(userDtoMap.isEmpty()){
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userDtoMap, new UserDTO(), false);
        stringRedisTemplate.expire(key,120, TimeUnit.MINUTES);
        //判斷是否登錄了,如果登錄了,則將其保存至綫程中
        UserHolder.saveUser(userDTO);
        return true;
    }
}
