package com.media.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.media.dto.Result;
import com.media.entity.ShopType;
import com.media.mapper.ShopTypeMapper;
import com.media.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.media.utils.RedisConstants.LOCK_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String value = stringRedisTemplate.opsForValue().get(LOCK_SHOP_TYPE_KEY);
        //2
        List<ShopType> typeList;
        if(StringUtils.isNotBlank(value)){
            typeList=JSONUtil.toList(value,ShopType.class);
            return Result.ok(typeList);
        }
        //3
        if("".equals(value)){
            return Result.fail("沒有該商店類型");
        }
        //4
        typeList = this
                .query().orderByAsc("sort").list();
        if(typeList.isEmpty()){
            stringRedisTemplate.opsForValue().set(LOCK_SHOP_TYPE_KEY,"");
            return Result.fail("沒有該商店類型");
        }
        stringRedisTemplate.opsForValue().set(LOCK_SHOP_TYPE_KEY, JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }
}
