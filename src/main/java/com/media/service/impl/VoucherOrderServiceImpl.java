package com.media.service.impl;

import com.media.dto.Result;
import com.media.entity.SeckillVoucher;
import com.media.entity.VoucherOrder;
import com.media.mapper.VoucherOrderMapper;
import com.media.service.ISeckillVoucherService;
import com.media.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.media.utils.RedisIdWorker;
import com.media.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //1根据id 去seckillvoucher 查
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if(seckillVoucher==null){
            return Result.fail("该优惠卷不存在");
        }
        //2 将现在的时间与开始时间和结束时间进行比较
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("该优惠卷还未开始进行售卖");
        }
        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("该优惠卷已经停止售卖");
        }
        //3 如果在范围内则可以下订单
        //这里是对用户的id加锁,意思是一个用户只能对一个优惠卷下单
        //如果对优惠卷加锁，会出现多个用户无法购买的情况
        RLock lock = redissonClient.getLock("lock:seckillVoucher" + UserHolder.getUser().getId());
        boolean b = lock.tryLock();
        //
        if(b!=true){
            return Result.fail("用户已经下过单了");
        }
        //3.1 这里要解决超卖问题,多并发
        //3.2 同时还要解决一人一单的问题
        //4 最后返回
        try {
            IVoucherOrderService poxy = (IVoucherOrderService) AopContext.currentProxy();
            return poxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId){
        Integer count = query().eq("user_id", UserHolder.getUser().getId()).eq("voucher_id", voucherId).count();
        if(count>0){
            return Result.fail("一个人只能下一单哦");
        }
        boolean update = seckillVoucherService.update().setSql("stock=stock-1").gt("stock", 0).eq("voucher_id", voucherId).update();
        if(update!=true){
            return Result.fail("优惠卷已经被抢完了");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(UserHolder.getUser().getId());
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setCreateTime(LocalDateTime.now());
        voucherOrder.setVoucherId(voucherId);
        this.save(voucherOrder);
        return Result.ok(orderId);
    }
}
