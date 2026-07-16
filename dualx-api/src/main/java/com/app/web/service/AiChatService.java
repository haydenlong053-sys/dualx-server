package com.app.web.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.app.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            1、你是一名金融风控专家,你需要判断用户出金是否正常，出金包含转账、提现。
            2、我告诉你用户修改密码时间，是提现还是转账，今日提现多少钱，是否异地登陆。
            3、回答必须简洁，简单说明一下：比如本次拒绝提币/转账，拒绝理由是什么，或者通过转账提币，通过理由是什么。
            4、返回格式需要返回json字符串，我需要转换成json返回给前端，里面包含code:(1通过和0未通过)、msg:文字说明
            """;

    private final ChatClient chatClient;

    public Map<String, Object> chat(String message) {
        String content = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
        return JSON.parseObject(content,new TypeReference<>() {});
    }

    public Flux<String> stream(String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .stream()
                .content();
    }




}
