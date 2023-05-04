package com.media.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.media.dto.Result;
import com.media.dto.UserDTO;
import com.media.entity.Blog;
import com.media.service.IBlogService;
import com.media.service.IUserService;
import com.media.utils.SystemConstants;
import com.media.utils.UserHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Resource
    private MongoTemplate mongoTemplate;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 获取登录用户
        // 返回id
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Query query=Query.query(Criteria.where("user_id").is(user.getId()));
        PageRequest of = PageRequest.of(current-1, SystemConstants.MAX_PAGE_SIZE, Sort.by("liked").descending());
        List<Blog> records = mongoTemplate.find(query.with(of), Blog.class);
      //  Page<Blog> page = blogService.query()
       //         .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
      //  List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogId(@PathVariable Integer id){
        return blogService.queryBlogId(id);
    }
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBlogLikes(id);
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

    @GetMapping("/of/follow")
    public Result listFollowBlog(@RequestParam(value = "lastId")Long lastId,@RequestParam(value = "offset",defaultValue = "0")Integer offset){
        return blogService.listFollowBlog(lastId,offset);
    }
}
