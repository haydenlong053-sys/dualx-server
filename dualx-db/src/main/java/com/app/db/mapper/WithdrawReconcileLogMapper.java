package com.app.db.mapper;

import com.app.db.entity.WithdrawReconcileLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * BSC链上提现成功事件表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-11
 */
@Mapper
public interface WithdrawReconcileLogMapper extends BaseMapper<WithdrawReconcileLog> {

    /**
     * 根据业务订单号选择性更新（只更新非 null 字段）
     * @param log 对账记录
     * @return 影响行数
     */
    int updateByOrderNumberSelective(WithdrawReconcileLog log);
}
