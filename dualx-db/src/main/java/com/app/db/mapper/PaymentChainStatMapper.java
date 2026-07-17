package com.app.db.mapper;

import com.app.db.entity.PaymentChainStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 链上充值统计快照表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Mapper
public interface PaymentChainStatMapper extends BaseMapper<PaymentChainStat> {

}
