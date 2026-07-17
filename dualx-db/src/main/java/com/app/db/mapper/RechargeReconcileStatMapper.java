package com.app.db.mapper;

import com.app.db.entity.RechargeReconcileStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 充值对账每日统计表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Mapper
public interface RechargeReconcileStatMapper extends BaseMapper<RechargeReconcileStat> {

    /**
     * 按币种统计充值对账数据
     */
    List<Map<String, Object>> statByToken(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("tokenAddress") String tokenAddress);
}
