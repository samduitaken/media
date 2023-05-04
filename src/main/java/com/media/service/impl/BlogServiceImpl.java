package com.media.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.media.dto.Result;
import com.media.dto.ScrollResult;
import com.media.dto.UserDTO;
import com.media.entity.Blog;
import com.media.entity.Follow;
import com.media.entity.User;
import com.media.mapper.BlogMapper;
import com.media.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.media.service.IFollowService;
import com.media.service.IUserService;
import com.media.utils.SystemConstants;
import com.media.utils.UserHolder;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.media.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.media.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private IFollowService followService;

    @Override
    public Result likeBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("用户未登录,请先登录");
        }
        //判断当前博客是否点赞
        String likeKey=BLOG_LIKED_KEY+id.toString();
        Long userId = UserHolder.getUser().getId();
        //从redis中去取，因为是用sortedset存储数据，所以用key和value来得到score，用sortedset是将点赞的用户进行排序，用时间进行排序
        Double isLiked = stringRedisTemplate.opsForZSet().score(likeKey, userId.toString());
        //如果score是空说明没点赞
        if(isLiked!=null){
           // boolean success = this.update().setSql("liked=liked-1").eq("id",id).update();
            //Criteria.where("id").is(id) 像是mp中query().eq()
            Query qu = new Query(Criteria.where("id").is(id));
            //如果要更新用updata,inc指的是在当前的数值 加 多少，-1 就是-1
            Update update = new Update().inc("liked", -1);
            UpdateResult updateResult = mongoTemplate.updateFirst(qu, update, Blog.class);
            if(updateResult!=null){
                stringRedisTemplate.delete(likeKey);
            }
        }else {
           // boolean success = this.update().setSql("liked=liked+1").eq("id",id).update();
            Query qu = new Query(Criteria.where("id").is(id));
            Update update = new Update().inc("liked", 1);
            UpdateResult updateResult = mongoTemplate.updateFirst(qu, update, Blog.class);
            if(updateResult!=null){
                //获得的是自1970-1-01 00:00:00.000 到当前时刻的时间距离,类型为long
                stringRedisTemplate.opsForZSet().add(likeKey,userId.toString(), System.currentTimeMillis());
            }
        }
        return Result.ok();
    }

    public Boolean blogIsLiked(Long id){
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return false;
        }
        //判断当前用用户是否对博客点过赞，没点过返回false
        Long userId = UserHolder.getUser().getId();
        String key=BLOG_LIKED_KEY+ id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score!=null){
            return true;
        }
        return false;
    }
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        //将首页Blog进行排序显示
        Query query=new Query();
        //分页的话则用PageRequest.of，再用query.with（of）来实现分页，而我们用分页时，得用页数-1
        PageRequest of = PageRequest.of(current-1, SystemConstants.MAX_PAGE_SIZE, Sort.by("liked").descending());
        List<Blog> records = mongoTemplate.find(query.with(of), Blog.class);
        System.out.println(records);
        //Page<Blog> page = query()
       //         .orderByDesc("liked")
        //        .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
       // List<Blog> records = page.getRecords();
        // 查询用户
        //将查询的到博客设置当前userid,设置昵称，设置头像，设置当前用户是否对这个blok是否点赞
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            blog.setIsLike(blogIsLiked(blog.getId()));
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogId(Integer id) {
        //根据博客id来取出博客, 得到相对应的博客
        Query qu = Query.query(Criteria.where("id").is(id));
        Blog blog= mongoTemplate.findOne(qu, Blog.class);
       // Blog blog = query().eq("id", id).one();
        //从mongodb中去去如果取出的博客为空则说明当前博客不存在
        if(blog==null){
            return Result.fail("该博客不存在");
        }
        //设置博客的islike属性,前端可以根据这个属性来判断当前用户是否对这个博客进行了点赞
        blog.setIsLike(blogIsLiked(blog.getId()));
        //设置博客相对应的用户和昵称
        queryBlogUser(blog);
        return Result.ok(blog);
    }
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryBlogLikes(Long id) {
        //从redis中去取相对应的博客Id的key
        String likeKey=BLOG_LIKED_KEY+id.toString();
        Set<String> likedRange = stringRedisTemplate.opsForZSet().range(likeKey, 0, 4);
        //如果取出的集合是空，说明没有人为这个博客点赞
        if(likedRange==null ||likedRange.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //将set集合遍历，set的value是userid,用Userid去数据库中找user的信息，再封装成userdto加入到arraylist中返回给前端
        ArrayList<UserDTO> userDtos = new ArrayList<>();
        for (String s : likedRange) {
            long userId = Long.parseLong(s);
            User byId = userService.getById(userId);
            UserDTO userDTO = BeanUtil.copyProperties(byId, UserDTO.class);
            userDtos.add(userDTO);
        }
        return Result.ok(userDtos);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        try {
            mongoTemplate.save(blog,"mycollection");
        } catch (Exception e) {
            return Result.fail("保存笔记失败");
        }
        //boolean save = save(blog);

        // 返回id
        //这里的key应该是user_id,而不是followuserid,下面是在数据库中找是谁关注了我
        List<Follow> followUsers = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow followUser : followUsers) {
            stringRedisTemplate.opsForZSet().add(FEED_KEY+followUser.getUserId(),blog.getId().toString(),System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result listFollowBlog(Long lastId, Integer offset) {
        Long id = UserHolder.getUser().getId();
        String feedKey=FEED_KEY+id;
        //reverseRangeByScoreWithScores指的是根据分数来查并且带上分数，这里的Min和count的参数是不变的，查出来的数据是set形式，里面value是Blogid,也就是我们当时存的blogid,还有score是time
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(feedKey, 0, lastId, offset, 3);
        if(typedTuples==null ||typedTuples.isEmpty()){
            return Result.ok();
        }
        ArrayList<Blog> blogs = new ArrayList<>(typedTuples.size());
        long lastTime=0L;
        int lastTimeNumber=1;
        //记录最小时间和次数
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            long scoreTime = typedTuple.getScore().longValue();
            Long  blogId= Long.valueOf(typedTuple.getValue());
            if(scoreTime==lastTime){
                lastTimeNumber++;
            }else {
                lastTime = scoreTime;
                lastTimeNumber=1;
            }
            Query qu = Query.query(Criteria.where("id").is(blogId));
            Blog byId  = mongoTemplate.findOne(qu, Blog.class);
           // Blog byId = this.getById(blogId);
            blogs.add(byId);
        }
        //将arryalist的Blog设置当前用户是否点过赞，还有blog的相关的用户信息
        for (Blog blog : blogs) {
            queryBlogUser(blog);
            checkBlogIsLiked(blog);
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setMinTime(lastTime);
        scrollResult.setOffset(lastTimeNumber);
        scrollResult.setList(blogs);
        return Result.ok(scrollResult);
    }

    private void checkBlogIsLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return;
        }
        Long userId = UserHolder.getUser().getId();
        String key=BLOG_LIKED_KEY+ blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }


}
