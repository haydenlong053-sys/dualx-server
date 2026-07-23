package com.app.web.service.impl;

import com.app.db.entity.WithdrawReconcileStat;
import com.app.db.mapper.WithdrawReconcileStatMapper;
import com.app.web.service.IWithdrawReconcileStatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 提现对账每日统计表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Service
@Slf4j
public class WithdrawReconcileStatServiceImpl extends ServiceImpl<WithdrawReconcileStatMapper, WithdrawReconcileStat> implements IWithdrawReconcileStatService {

    @Autowired
    private WithdrawReconcileStatMapper withdrawReconcileStatMapper;

    /**
     * 统计前一天数据
     */
    @Override
    public void statPreviousDay() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = statDate.atStartOfDay();
        LocalDateTime endOfDay = statDate.plusDays(1).atStartOfDay();
        log.info("开始统计提现对账, statDate={}", statDate);

        // 链桥USDT
        List<Map<String, Object>> bridgeUsdtStatus1Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "U", 1);
        saveOrUpdateStat(statDate, 1, "U", bridgeUsdtStatus1Result, 1);
        //IM 的USDT
        List<Map<String, Object>> bridgeUsdtStatus2Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "U", 2);
        saveOrUpdateStat(statDate, 1, "U", bridgeUsdtStatus2Result, 2);
        // 链桥老系统USDT
        List<Map<String, Object>> bridgeUsdtStatus3Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "U", 3);
        saveOrUpdateStat(statDate, 1, "U", bridgeUsdtStatus3Result, 3);
        // 美区USDT
        List<Map<String, Object>> bridgeUsdtStatus4Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "U", 3);
        saveOrUpdateStat(statDate, 1, "U", bridgeUsdtStatus4Result, 4);

        // 链桥DUALX（老系统状态码1）
        List<Map<String, Object>> bridgeOdicStatus1Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "DUALX", 1);
        saveOrUpdateStat(statDate, 2, "DUALX", bridgeOdicStatus1Result, 1);
        // IM 的DUALX
        List<Map<String, Object>> bridgeOdicStatus2Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "DUALX", 2);
        saveOrUpdateStat(statDate, 1, "DUALX", bridgeOdicStatus2Result, 2);
        // 链桥老系统
        List<Map<String, Object>> bridgeOdicStatus3Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "DUALX", 3);
        saveOrUpdateStat(statDate, 2, "DUALX", bridgeOdicStatus3Result, 4);
        // 美区
        List<Map<String, Object>> bridgeOdicStatus4Result = withdrawReconcileStatMapper.statByCoinId(startOfDay, endOfDay, "DUALX", 4);
        saveOrUpdateStat(statDate, 2, "DUALX", bridgeOdicStatus4Result, 4);
        log.info("提现对账统计完成, statDate={}", statDate);
    }

    /**
     * 保存或更新统计
     */
    private void saveOrUpdateStat(LocalDate statDate, Integer coinId, String coinName, List<Map<String, Object>> resultList, Integer originType) {
        // 查询是否已存在
        LambdaQueryWrapper<WithdrawReconcileStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WithdrawReconcileStat::getStatDate, statDate)
                .eq(WithdrawReconcileStat::getCoinId, coinId)
                .eq(WithdrawReconcileStat::getOriginType, originType);
        WithdrawReconcileStat exist = this.getOne(wrapper);
        // 构建统计实体
        WithdrawReconcileStat stat = new WithdrawReconcileStat();
        stat.setStatDate(statDate);
        stat.setCoinId(coinId);
        stat.setCoinName(coinName);
        stat.setUpdateTime(LocalDateTime.now());
        stat.setOriginType(originType);
        // 如果有统计数据，使用实际值
        if (resultList != null && !resultList.isEmpty()) {
            Map<String, Object> data = resultList.get(0);
            Integer totalCount = ((Number) data.get("totalCount")).intValue();
            stat.setTotalCount(totalCount);
            stat.setTotalAmount((BigDecimal) data.get("totalAmount"));
            stat.setSuccessCount(((Number) data.get("successCount")).intValue());
            stat.setSuccessAmount((BigDecimal) data.get("successAmount"));
            stat.setExceptionCount(((Number) data.get("exceptionCount")).intValue());
            stat.setExceptionAmount((BigDecimal) data.get("exceptionAmount"));
            log.info("有提现数据, coinId={}, statDate={}, total={}", coinId, statDate, totalCount);
        } else {
            // 没有数据，保存默认值0
            stat.setTotalCount(0);
            stat.setTotalAmount(BigDecimal.ZERO);
            stat.setSuccessCount(0);
            stat.setSuccessAmount(BigDecimal.ZERO);
            stat.setExceptionCount(0);
            stat.setExceptionAmount(BigDecimal.ZERO);
            log.info("无提现数据, 保存默认0值, coinId={}, statDate={}", coinId, statDate);
        }
        // 插入或更新
        if (exist != null) {
            stat.setId(exist.getId());
            stat.setCreateTime(exist.getCreateTime());
            this.updateById(stat);
            log.info("更新统计: coinId={}, statDate={}", coinId, statDate);
        } else {
            stat.setCreateTime(LocalDateTime.now());
            this.save(stat);
            log.info("插入统计: coinId={}, statDate={}", coinId, statDate);
        }
    }
}
