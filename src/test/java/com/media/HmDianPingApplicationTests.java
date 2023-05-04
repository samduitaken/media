package com.media;

import com.media.entity.Shop;
import com.media.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.media.utils.RedisConstants.USER_SIGN_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;

    @Resource
    private MongoTemplate mongoTemplate;
    @Test
    void loadShopData(){
        List<Shop> list = shopService.list();
        Map<Long,List<Shop>> map=list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();
            List<Shop> value = entry.getValue();
            String key="shop:geo:"+typeId;
            List<RedisCommands.GeoLocation<String>> geoLocations = new ArrayList<>();
            for (Shop shop : value) {
                geoLocations.add(new RedisCommands.GeoLocation<String>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
            }
            System.out.println(geoLocations);
            stringRedisTemplate.opsForGeo().add(key,geoLocations);
        }
    }
    @Test
    void sign(){
        LocalDateTime now = LocalDateTime.now();
        String key=USER_SIGN_KEY+1010+now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        int dayofmonth=now.getDayOfMonth();
        //用Bitfiled 的方法得到一个集合,集合里面是十进制的数字,我们用get(0)变成一个数字,再用数字和1去做与运算
        List<Long> results = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayofmonth))
                        .valueAt(0));
        System.out.println("results的值："+results);
        if(results==null || results.isEmpty()){
            System.out.println(0);
        }
        Long aLong = results.get(0);
        System.out.println("along的值："+aLong);
        if(aLong==null||aLong==0){
            System.out.println(0);
        }
        int count=0;
        while (true){
            if((aLong & 1)==0){
                break;
            }else {
                count++;
            }
            aLong >>>=1;
        }
        System.out.println("连续签到次数:"+count);
    }
    @Test
    void sign2(){
        LocalDateTime now = LocalDateTime.now();
        String key=USER_SIGN_KEY+1010+now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        int dayofmonth=now.getDayOfMonth();
        stringRedisTemplate.opsForValue().setBit(key,dayofmonth-1,true);
    }




}
