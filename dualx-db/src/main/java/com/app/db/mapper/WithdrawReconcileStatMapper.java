package com.app.db.mapper;

import com.app.db.entity.WithdrawReconcileStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 提现对账每日统计表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Mapper
public interface WithdrawReconcileStatMapper extends BaseMapper<WithdrawReconcileStat> {

    /**
     * 按币种统计提现对账数据
     */
    List<Map<String, Object>> statByCoinId(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("type") String type,
                                           @Param("originType") Integer originType);
}
