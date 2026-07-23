package com.app.web.service.impl;

import com.app.db.entity.RechargeReconcileStat;
import com.app.db.mapper.RechargeReconcileStatMapper;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IRechargeReconcileStatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 充值对账每日统计表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Service
@Slf4j
public class RechargeReconcileStatServiceImpl extends ServiceImpl<RechargeReconcileStatMapper, RechargeReconcileStat> implements IRechargeReconcileStatService {

    @Resource
    private RechargeReconcileStatMapper statMapper;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    /**
     * 统计前一天数据
     */
    public void statPreviousDay() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        LocalDateTime startTime = statDate.atStartOfDay();
        LocalDateTime endTime = statDate.plusDays(1).atStartOfDay();

        log.info("开始统计充值对账, statDate={}", statDate);

        // 统计 USDT
        List<Map<String, Object>> usdtResult = statMapper.statByToken(startTime, endTime, withdrawContractConfig.getUsdtContract());
        saveOrUpdateStat(statDate, 1, "USDT", usdtResult);

        // 统计 DUALX
        List<Map<String, Object>> odicResult = statMapper.statByToken(startTime, endTime, withdrawContractConfig.getOdicContract());
        saveOrUpdateStat(statDate, 2, "DUALX", odicResult);

        // 统计 DUALX
        List<Map<String, Object>> duonResult = statMapper.statByToken(startTime, endTime, withdrawContractConfig.getDuonContract());
        saveOrUpdateStat(statDate, 3, "DUON", duonResult);

        log.info("充值对账统计完成, statDate={}", statDate);
    }

    /**
     * 保存或更新统计
     */
    private void saveOrUpdateStat(LocalDate statDate, Integer coinId, String coinName, List<Map<String, Object>> resultList) {
        // 查询是否已存在
        LambdaQueryWrapper<RechargeReconcileStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeReconcileStat::getStatDate, statDate)
                .eq(RechargeReconcileStat::getCoinId, coinId);
        RechargeReconcileStat exist = statMapper.selectOne(wrapper);
        // 构建统计实体
        RechargeReconcileStat stat = new RechargeReconcileStat();
        stat.setStatDate(statDate);
        stat.setCoinId(coinId);
        stat.setCoinName(coinName);
        stat.setUpdateTime(LocalDateTime.now());
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
            log.info("有统计数据, coinId={}, statDate={}, total={}", coinId, statDate, totalCount);
        } else {
            // 没有数据，保存默认值0
            stat.setTotalCount(0);
            stat.setTotalAmount(BigDecimal.ZERO);
            stat.setSuccessCount(0);
            stat.setSuccessAmount(BigDecimal.ZERO);
            stat.setExceptionCount(0);
            stat.setExceptionAmount(BigDecimal.ZERO);
            log.info("无统计数据, 保存默认0值, coinId={}, statDate={}", coinId, statDate);
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
