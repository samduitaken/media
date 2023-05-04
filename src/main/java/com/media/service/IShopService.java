package com.media.service;

import com.media.dto.Result;
import com.media.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result quertShopByType(Integer typeId, Integer current,Double x,Double y);

    Result queryShopById(Long id);
}
