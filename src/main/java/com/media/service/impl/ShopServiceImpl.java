package com.media.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.media.dto.Result;
import com.media.entity.Shop;
import com.media.mapper.ShopMapper;
import com.media.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.media.utils.SystemConstants;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.media.utils.RedisConstants.*;
import static com.media.utils.SystemConstants.MAX_PAGE_CURRENT;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result quertShopByType(Integer typeId, Integer current,Double x,Double y) {
        String key=CACHE_SHOP_TYPE_KEY+typeId+":"+current;
        if(x==null || y==null){
            // 根据类型分页查询
            String shopTypeValue = stringRedisTemplate.opsForValue().get(key);
            if("".equals(shopTypeValue)){
                return Result.fail("没有该参数");
            }
            if(current>=MAX_PAGE_CURRENT){
                return Result.fail("已经是最大页了");
            }
            if(shopTypeValue!=null ){
                List<Shop> shops = JSONUtil.toList(shopTypeValue, Shop.class);
                return Result.ok(shops);
            }
            Page<Shop> page = this.query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            if(page.getRecords()==null || page.getRecords().isEmpty()){
                MAX_PAGE_CURRENT=current;
                stringRedisTemplate.opsForValue().set(key,"",2,TimeUnit.MINUTES);
                return Result.fail("底下没有了哦,喵！");
            }
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(page.getRecords()),2, TimeUnit.MINUTES);
            // 返回数据

            return Result.ok(page.getRecords());
        }
        //这里是计算第几页查几条数据
        //比如第一页,1 , from就是0，end就是5，意思是从0 条数据查,查到5 条
        int from=(current-1)* SystemConstants.DEFAULT_PAGE_SIZE;
        int end=current*SystemConstants.DEFAULT_PAGE_SIZE;
       String geoKey= "shop:geo:"+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(geoKey,
                //意思是将当前的xy作为圆心查附件的商铺距离
                GeoReference.fromCoordinate(x, y),
                //查附近的5000米的距离
                new Distance(5000),
                //指带上距离，并且指定要查多少条
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        //如果results未空，说明没有查出附件商铺
        if(results==null){
            return Result.ok();
        }
        //result中的content就是当时我们存的arraylist,里面是RedisGeoCommands.GeoLocation,里面有name,和point坐标
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size()<=from ){
            return Result.ok(Collections.emptyList());
        }

        ArrayList<Long> ids = new ArrayList<>();
        HashMap<String, Distance> hashMap = new HashMap<>();
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list1=null;
        //截取数据，因为results截取的是全部数据，我们得做分页查询，即跳过多少数据
        list1 = list.subList(from,list.size());
        ArrayList<Shop> shops = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : list1) {
            //得到距离
            Distance distance = result.getDistance();
            //得到商店的id
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));
            //根据商店的id数据库中查shop
            Shop byId = getById(shopId);
            //设置每个shop的距离
            byId.setDistance(distance.getValue());
            shops.add(byId);
        }


/*        list.stream().skip(from).forEach(result->{
            Distance distance = result.getDistance();
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));
            hashMap.put(shopId,distance);
        });
        String idstr = StrUtil.join(",",ids);
        List<Shop> shops =query().in("id",ids).last("ORDER BY FIELD(id,"+idstr+")").list();
        for (Shop shop : shops) {
            shop.setDistance(hashMap.get(shop.getId().toString()).getValue());
        }*/
        return Result.ok(shops);

    }

    @Override
    public Result queryShopById(Long id) {
        //解决缓存击穿问题
        String key=CACHE_SHOP_KEY+id;
        String shopValue = stringRedisTemplate.opsForValue().get(key);
        Shop shop=null;
        if(shopValue!=null){
             shop = JSONUtil.toBean(shopValue, Shop.class);
            return Result.ok(shop);
        }
        if("".equals(shopValue)){
            return Result.fail("该商户不存在");
        }
        //这里解决的是缓存的击穿问题, 可以用redis的setnx做
        //也可以用redisson来做
        String lockKey="lock:ShopById:" + id;
        RLock lock=null;
        try {
            lock = redissonClient.getLock(lockKey);
            //这里是获得key的线程才可以往下走,否则在先等50毫秒,再通过递归再次获得尝试获得锁
            boolean IsTakeLock = lock.tryLock(50, 2000, TimeUnit.MILLISECONDS);
            if(IsTakeLock!=true){
                queryShopById(id);
            }
            shop = query().eq("id",id).one();
            if(shop==null){
                stringRedisTemplate.opsForValue().set(key,"",5,TimeUnit.MINUTES);
                return Result.fail("商户不存在");
            }
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),30,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return Result.ok(shop);
    }
}
