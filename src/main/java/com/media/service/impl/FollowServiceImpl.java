package com.media.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.media.dto.Result;
import com.media.dto.UserDTO;
import com.media.entity.Follow;
import com.media.mapper.FollowMapper;
import com.media.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.media.service.IUserService;
import com.media.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long id, Boolean isFollow) {
        //检查当前线程是否有用户，没有让他先登录
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("请先登录");
        }
        //将用户的id取出
        Long loginUserId = UserHolder.getUser().getId();
        //redis的key 当前用户的id作为Key
        String key="follows:"+loginUserId;
        //isFollow是由前端传过来
        if(isFollow){
            Follow follow = new Follow();
            follow.setFollowUserId(id);
            follow.setUserId(loginUserId);
            boolean isSave = save(follow);
            if(isSave){
                stringRedisTemplate.opsForSet().add(key,id.toString());
            }
            return Result.ok("关注成功");
        }
        //如果已经关注了则删除对应的Key和数据库中的数据
        remove(new LambdaQueryWrapper<Follow>().eq(Follow::getUserId,loginUserId).eq(Follow::getFollowUserId,id));
        stringRedisTemplate.delete(key);
        return Result.ok("取关成功");

    }

    @Override
    public Result isFollow(Long id) {

        UserDTO user = UserHolder.getUser();

        if(user==null){
            return Result.ok(false);
        }
        //将当前用户的Id拿到
        Long loginUserId = UserHolder.getUser().getId();
        //查看数据库当前用户是不是关注了目标用户，如果没关注则返回的follow是一个空对象，我们则返回false
        Follow one = query().eq("user_id", loginUserId).eq("follow_user_id", id).one();
        return Result.ok(one!=null);
    }

    @Override
    public Result commonFollow(Long id) {
        //拿到当前用户的Id
        Long loginUserId = UserHolder.getUser().getId();
        //第一Key为当前用户的Id
        //第二个则是我们要查看的用户，我们要查看这个用户和当前用户共同关注了哪些人
        String key="follows:"+loginUserId;
        String key2="follows:"+id;
        //使用set方法中交集，得到一个set对象
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        //如果set为空说明这个人和当前用户没有共同关注的对象，直接返回给一个空集合
        if(intersect==null || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //因为我们在set中存放的是一个一个用的Id,所以我们得把这些Id拿出来，在去数据库中找，找到后封装成一个list，返回给前端，最后将里面的user转化为一个一个的userDto
        List<UserDTO> collect = userService.listByIds(intersect).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(collect);
    }
}
