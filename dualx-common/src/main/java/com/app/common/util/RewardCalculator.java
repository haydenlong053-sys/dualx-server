package com.app.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RewardCalculator {

    /**
     * 计算收益
     * @param totalDays         剩余多少天
     * @param totalReward       剩余多少收益
     * @param fluctuationRate    每日浮动比例
     * @return  返回当日收益总金额
     */
    public static BigDecimal calculateTheProfit(Integer totalDays,BigDecimal totalReward,BigDecimal fluctuationRate) {
        BigDecimal[] dailyRewards = new BigDecimal[totalDays]; // 存储每天的奖励
        Random random = new Random();

        BigDecimal totalDistributed = BigDecimal.ZERO; // 记录已分配的总奖励

        for (int day = 0; day < totalDays; day++) {
            // 计算基础收益
            BigDecimal dailyBaseReward = totalReward.divide(new BigDecimal(totalDays), 6, RoundingMode.HALF_UP);

            // 生成 -0.5% ~ 0.5% 之间的浮动比例
            BigDecimal fluctuation = new BigDecimal(random.nextDouble())
                .multiply(fluctuationRate.multiply(new BigDecimal("2"))) // 生成 0~1，然后放大到 0~1%
                .subtract(fluctuationRate); // 再减去 0.5%，得到 -0.5% ~ 0.5% 之间的浮动值

            // 计算浮动后的收益
            BigDecimal dailyReward = dailyBaseReward.multiply(BigDecimal.ONE.add(fluctuation))
                .setScale(6, RoundingMode.HALF_UP);

            // 记录数据
            dailyRewards[day] = dailyReward;
            totalDistributed = totalDistributed.add(dailyReward);
        }

        // **误差调整** (确保 300 天收益总和 2000)
        BigDecimal adjustment = totalReward.subtract(totalDistributed);
        dailyRewards[totalDays - 1] = dailyRewards[totalDays - 1].add(adjustment)
            .setScale(6, RoundingMode.HALF_UP);

        // **分配 24 次收益**
        for (int day = 0; day < totalDays; day++) {
            BigDecimal dailyReward = dailyRewards[day];
            return dailyReward;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 将收益分成24份不一致的收益
     * @param totalAmount
     * @return
     */
    public static List<BigDecimal> dayIncome(BigDecimal totalAmount,int numParts) {
        // 分配金额
        BigDecimal[] distributedAmounts = distributeAmount(totalAmount, numParts);
        // 输出分配结果
        List<BigDecimal> list = new ArrayList<BigDecimal>();
        for (int i = 0; i < distributedAmounts.length; i++) {
            list.add(distributedAmounts[i]);
        }
        return list;
    }

    /**
     * 将一个数字分成 N 份，确保每一份不同且总和为目标值。
     */
    public static BigDecimal[] distributeAmount(BigDecimal totalAmount, int numParts) {
        BigDecimal[] amounts = new BigDecimal[numParts];
        Random random = new Random();
        BigDecimal sumOfWeights = BigDecimal.ZERO;

        // 生成 N 个随机数并计算总权重
        BigDecimal[] weights = new BigDecimal[numParts];
        for (int i = 0; i < numParts; i++) {
            weights[i] = new BigDecimal(random.nextDouble()).setScale(10, RoundingMode.HALF_UP); // 随机生成一个 0~1 之间的小数
            sumOfWeights = sumOfWeights.add(weights[i]);
        }

        // 根据权重分配金额，保证总和为 totalAmount
        for (int i = 0; i < numParts; i++) {
            amounts[i] = weights[i].divide(sumOfWeights, 10, RoundingMode.HALF_UP)
                .multiply(totalAmount)
                .setScale(10, RoundingMode.HALF_UP); // 保留两位小数
        }
        return amounts;
    }
}
