package com.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.media.dto.Result;
import com.media.entity.Voucher;
import com.media.mapper.VoucherMapper;
import com.media.entity.SeckillVoucher;
import com.media.service.ISeckillVoucherService;
import com.media.service.IVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
        System.out.println("顶顶顶："+shopId);
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        for (Voucher voucher : vouchers) {
            voucher.setBeginTime(LocalDateTime.of(2022,01,04,9,24));
            voucher.setEndTime(LocalDateTime.of(2025,01,04,9,24));
        }
        System.out.println(vouchers);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }
}
