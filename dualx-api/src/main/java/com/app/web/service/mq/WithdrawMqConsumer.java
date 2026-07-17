package com.app.web.service.mq;

import com.alibaba.fastjson.JSONObject;
import com.app.db.entity.BscWithdrawalLog;
import com.app.web.config.RabbitMqConfig;
import com.app.web.service.IBscWithdrawalLogService;
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
    private IBscWithdrawalLogService bscWithdrawalLogService;

    @PostConstruct
    public void init() {
        log.info("===== WithdrawMqConsumer 初始化完成，监听队列：{} =====", RabbitMqConfig.BSC_WITHDRAWAL_LOG_QUEUE);
    }

    /**
     * 接收提现消息 - 队列1
     */
    @RabbitListener(queues = RabbitMqConfig.BSC_WITHDRAWAL_LOG_QUEUE)
    public void receiveWithdrawMessage(Message message, Channel channel) {
        processMessage(message, channel, RabbitMqConfig.BSC_WITHDRAWAL_LOG_QUEUE);
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
            if (RabbitMqConfig.BSC_WITHDRAWAL_LOG_QUEUE.equalsIgnoreCase(queueName)) {
                BscWithdrawalLog bscWithdrawalLog = JSONObject.parseObject(body, BscWithdrawalLog.class);
                bscWithdrawalLogService.saveOrUpdate(bscWithdrawalLog);
                // 手动确认消息
                channel.basicAck(deliveryTag, false);
            }
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