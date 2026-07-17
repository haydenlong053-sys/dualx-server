package com.app.web.service.mq;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;



@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class WithdrawMqProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     *
     * @param body 消息内容（JSON字符串）
     * @param queue 队列名称
     */
    public void send(String body, String queue) {
        if (!StringUtils.isBlank(body)) {
            if (StringUtils.isBlank(queue)) {
                log.warn("发送MQ消息失败: 队列名称为空");
                return;
            }
            try {
                rabbitTemplate.convertAndSend(queue, body);
                log.info("发送MQ消息成功, queue={}, body={}", queue, body);
            } catch (Exception e) {
                log.error("发送MQ消息失败, queue={}, body={}", queue, body, e);
                throw new RuntimeException("发送MQ消息失败", e);
            }
        } else {
            log.warn("发送MQ消息失败: 消息内容为空");
        }
    }
}