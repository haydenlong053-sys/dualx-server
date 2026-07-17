package com.app.db.mapper;

import com.app.db.entity.RechargeReconcileLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 支付订单对账中心表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Mapper
public interface RechargeReconcileLogMapper extends BaseMapper<RechargeReconcileLog> {

    /**
     * 根据业务订单号选择性更新（只更新非 null 字段）
     * @param log 对账记录
     * @return 影响行数
     */
    int updateByOrderNumberSelective(RechargeReconcileLog log);
}
