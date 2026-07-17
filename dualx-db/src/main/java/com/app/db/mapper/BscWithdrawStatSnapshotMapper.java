package com.app.db.mapper;

import com.app.db.entity.BscWithdrawStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * BSC出账统计快照表 Mapper 接口
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Mapper
public interface BscWithdrawStatSnapshotMapper extends BaseMapper<BscWithdrawStat> {

}
