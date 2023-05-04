package com.media.service;

import com.media.dto.Result;
import com.media.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result likeBlog(Long id);

    Result queryHotBlog(Integer current);

    Result queryBlogId(Integer id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result listFollowBlog(Long lastId, Integer offset);
}
