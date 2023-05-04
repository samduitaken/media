package com.media.controller;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.media.dto.LoginFormDTO;
import com.media.dto.Result;
import com.media.dto.UserDTO;
import com.media.entity.Blog;
import com.media.entity.User;
import com.media.entity.UserInfo;
import com.media.service.IBlogService;
import com.media.service.IUserInfoService;
import com.media.service.IUserService;
import com.media.utils.SystemConstants;
import com.media.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private IBlogService blogService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // TODO 发送短信验证码并保存验证码
        return userService.sendCode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // TODO 实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return userService.logout();
    }

    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
    @GetMapping("/of/user")
    public  Result listBlog(@RequestParam(value = "id")Integer id,@RequestParam(value = "current") Integer current){
        Page<Blog> blogPage = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.eq(Blog::getUserId,id);
        blogLambdaQueryWrapper.orderByDesc(Blog::getUpdateTime);
        blogService.page(blogPage,blogLambdaQueryWrapper);
        return Result.ok(blogPage.getRecords());
    }
    @GetMapping("/{id}")
    public Result userIndex(@PathVariable Integer id){
        User byId = userService.getById(id);
        if(byId==null){
            return Result.fail("用户不存在");
        }
        UserDTO userDTO = BeanUtil.copyProperties(byId, UserDTO.class);
        return Result.ok(userDTO);
    }

}
