package com.app.web.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列配置
 */
@Configuration
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class RabbitMqConfig {

    /**
     * 用户购买成功的队列（同时用于请求和确认消息）
     */
    public static final String USER_PURCHASE_SUCCESS_QUEUE = "user.purchase.success.queue";

    /**
     * 用户购买成功的队列（同时用于请求和确认消息）
     */
    public static final String IM_USER_PURCHASE_SUCCESS_QUEUE = "im.user.purchase.success.queue";

    /**
     * 美区用户购买成功的队列（同时用于请求和确认消息）
     */
    public static final String USER_PURCHASE_SUCCESS_QUEUE_US = "user.purchase.success.queue.us";

    /**
     * PRODUCT区用户购买成功的队列（同时用于请求和确认消息）
     */
    public static final String USER_PURCHASE_SUCCESS_QUEUE_PRODUCT = "user.purchase.success.queue.product";


    /**
     * 支付成功订单同步队列
     */
    public static final String ORDER_SYNC_QUEUE = "order.sync.queue";

    /**
     *  提现成功或失败订单同步队列
     */
    public static final String WITHDRAW_SYNC_QUEUE = "withdraw.sync.queue";

    /**
     *  提现成功或失败订单同步队列
     */
    public static final String USER_RECHARGE_SUCCESS_QUEUE = "user.recharge.success.queue";




     /**
     * 提现记录同步 队列
     */
    public static final String BSC_WITHDRAWAL_LOG_QUEUE = "bsc.withdrawal.log.queue";





    @Bean
    public Queue userPurchaseSuccessQueue() {
        // 队列名, 持久化, 非独占, 不自动删除
        return new Queue(USER_PURCHASE_SUCCESS_QUEUE, true, false, false);
    }

    @Bean
    public Queue orderSyncQueue() {
        // 队列名, 持久化, 非独占, 不自动删除
        return new Queue(ORDER_SYNC_QUEUE, true, false, false);
    }

    @Bean
    public Queue withdrawSyncQueue() {
        // 队列名, 持久化, 非独占, 不自动删除
        return new Queue(WITHDRAW_SYNC_QUEUE, true, false, false);
    }

    @Bean
    public Queue userRechargeSuccessQueue() {
        // 队列名, 持久化, 非独占, 不自动删除
        return new Queue(USER_RECHARGE_SUCCESS_QUEUE, true, false, false);
    }

    @Bean
    public Queue bscWithdrawalLogQueue() {
        return new Queue(BSC_WITHDRAWAL_LOG_QUEUE, true, false, false);
    }
}