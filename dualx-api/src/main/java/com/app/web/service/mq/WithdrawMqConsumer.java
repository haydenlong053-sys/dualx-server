package com.app.web.service.mq;

import com.app.web.config.RabbitMqConfig;
import com.app.web.service.IPaymentReconcileLogService;
import com.app.web.service.IRechargeReconcileLogService;
import com.app.web.service.IWithdrawReconcileLogService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;



import java.nio.charset.StandardCharsets;


/**
 * 提现消息消费者
 */
@Slf4j
@Component
public class WithdrawMqConsumer {

    @Resource
    private IWithdrawReconcileLogService withdrawReconcileLogService;

    @Resource
    private IRechargeReconcileLogService rechargeReconcileLogService;

    @Resource
    private IPaymentReconcileLogService paymentReconcileLogService;


    @PostConstruct
    public void init() {
        log.info("===== WithdrawMqConsumer 初始化完成，监听队列：{}, {},{} =====",
                RabbitMqConfig.CHAIN_SCAN_DEPOSIT_USER,
                RabbitMqConfig.CHAIN_SCAN_PAYMENT_USER,
                RabbitMqConfig.CHAIN_SCAN_WITHDRAW_USER);
    }

    /**
     * 接收提现消息 - 队列1
     */
    @RabbitListener(queues = RabbitMqConfig.CHAIN_SCAN_DEPOSIT_USER)
    public void receiveWithdrawMessage(Message message, Channel channel) {
        processMessage(message, channel, RabbitMqConfig.CHAIN_SCAN_DEPOSIT_USER);
    }

    /**
     * 接收提现消息 - 队列2
     */
    @RabbitListener(queues = RabbitMqConfig.CHAIN_SCAN_PAYMENT_USER)
    public void receiveWithdrawMessage1(Message message, Channel channel) {
        processMessage(message, channel, RabbitMqConfig.CHAIN_SCAN_PAYMENT_USER);
    }


    /**
     * 接收提现消息 - 队列2
     */
    @RabbitListener(queues = RabbitMqConfig.CHAIN_SCAN_WITHDRAW_USER)
    public void receiveWithdrawMessage2(Message message, Channel channel) {
        processMessage(message, channel, RabbitMqConfig.CHAIN_SCAN_WITHDRAW_USER);
    }

    /**
     * 统一消息处理方法
     */
    private void processMessage(Message message, Channel channel, String queueName) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = "";
        try {
            body = new String(message.getBody(), StandardCharsets.UTF_8);
            //充值信息
            if (RabbitMqConfig.CHAIN_SCAN_DEPOSIT_USER.equalsIgnoreCase(queueName)) {
                rechargeReconcileLogService.processMessage(body);
            } else if (RabbitMqConfig.CHAIN_SCAN_PAYMENT_USER.equalsIgnoreCase(queueName)) {
                paymentReconcileLogService.processMessage(body);
            } else if (RabbitMqConfig.CHAIN_SCAN_WITHDRAW_USER.equalsIgnoreCase(queueName)) {
                withdrawReconcileLogService.processMessage(body);
            }
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.info("失败的消息为: {}", body);
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("MQ消息处理异常", ex);
            }
        }
    }
}