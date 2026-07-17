package com.app.db.mapper;

import com.app.db.entity.PaymentReconcileStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 支付对账每日统计表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Mapper
@SuppressWarnings("unused")
public interface PaymentReconcileStatMapper extends BaseMapper<PaymentReconcileStat> {

    /**
     * 按币种统计支付对账数据
     */
    List<Map<String, Object>> statByToken(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("tokenAddress") String tokenAddress);
}
