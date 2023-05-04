package com.media.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.media.dto.LoginFormDTO;
import com.media.dto.Result;
import com.media.dto.UserDTO;
import com.media.entity.User;
import com.media.mapper.UserMapper;
import com.media.service.IUserService;
import com.media.utils.RegexUtils;
import com.media.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.media.utils.RedisConstants.*;
import static com.media.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private String userKey;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(phone)){
            //校验手机格式是否正确
            return Result.fail("手机格式不符合规则,请检查");
        }
        if(StringUtils.isBlank(phone)){
            return Result.fail("短信发送失败,请检查手机号码是否正确");
        }
        String codekey=LOGIN_CODE_KEY+phone;
        String code= RandomUtil.randomNumbers(4);
        log.debug("发送的验证码是:"+code);

        stringRedisTemplate.opsForValue().set(codekey,code,3, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(LOGIN_PHONE_KEY+phone,phone,3, TimeUnit.MINUTES);
        return Result.ok("验证码发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String codeKey=LOGIN_CODE_KEY+loginForm.getPhone();
        String redisCode = stringRedisTemplate.opsForValue().get(codeKey);
        String redisPhone = stringRedisTemplate.opsForValue().get(LOGIN_PHONE_KEY + loginForm.getPhone());
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机格式不正确,请检查");
        }
        if(!(loginForm.getPhone().equals(redisPhone)) || StringUtils.isBlank(loginForm.getPhone()) || loginForm.getPhone()==null ){
            return Result.fail("手机号不正确,请检查手机号");
        }
        if(!(loginForm.getCode().equals(redisCode))||StringUtils.isBlank(loginForm.getCode())||loginForm.getCode()==null) {
            return Result.fail("验证码不正确,请检查验证码");
        }
        User user = this.query().eq("phone", loginForm.getPhone()).one();
        if(user==null){
            user=createUser(loginForm.getPhone());
        }
        String token= UUID.randomUUID().toString();
        userKey=LOGIN_USER_KEY+token;

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //将userdto 转化为map, 属性名做为hash 的key, 属性值为value,hash结构可以对单个字段做crud,并且内存占用更少
        //CopyOptions.create().setFieldValueEditor是自定义转化规则
        Map<String, Object> userDtoMap = BeanUtil.beanToMap(userDTO,new HashMap<>(16), CopyOptions.create()
                                .setFieldValueEditor((filedName,filedValue)->filedValue.toString()));

        stringRedisTemplate.opsForHash().putAll(userKey,userDtoMap);
        stringRedisTemplate.expire(userKey,30,TimeUnit.MINUTES);
        //做登录校验时,如果我们将用户信息放在session中,那么tomcat会自动将session的id写到cookie中,下次请求就会带着cookie,会根据id找session
        //而如果我们使用redis时,往往会将token做为key,value为hash存储user对象,但是tomcat不会自动把token交给前端,所以我们要手动返回token
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        UserHolder.removeUser();
        //注意这里万一传过来的是空key，则可能报错
        if (userKey != null && !"".equals(userKey)) {
            stringRedisTemplate.delete(userKey);
        }
       return Result.ok("退出登录");
    }

    public User createUser(String phone){
            User user = new User();
            user.setPhone(phone);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(6));
            this.save(user);
            return user;
        }


}
