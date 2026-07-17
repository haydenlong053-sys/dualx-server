package com.app.web.service.impl;

import com.app.db.entity.PaymentChainStat;
import com.app.db.mapper.PaymentChainStatMapper;
import com.app.web.service.IPaymentChainStatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 链上充值统计快照表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Service
public class PaymentChainStatServiceImpl extends ServiceImpl<PaymentChainStatMapper, PaymentChainStat> implements IPaymentChainStatService {

}
