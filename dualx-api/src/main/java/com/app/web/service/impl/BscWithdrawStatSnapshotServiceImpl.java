package com.app.web.service.impl;

import com.app.db.entity.BscWithdrawStat;
import com.app.db.mapper.BscWithdrawStatSnapshotMapper;
import com.app.web.service.IBscWithdrawStatSnapshotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * BSC出账统计快照表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Service
public class BscWithdrawStatSnapshotServiceImpl extends ServiceImpl<BscWithdrawStatSnapshotMapper, BscWithdrawStat> implements IBscWithdrawStatSnapshotService {

}
