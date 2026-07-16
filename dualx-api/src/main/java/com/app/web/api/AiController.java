package com.app.web.api;

import com.alibaba.fastjson.JSON;
import com.app.db.entity.User;
import com.app.web.api.req.AiChatReq;
import com.app.web.service.AiChatService;
import com.app.web.service.IUserService;
import com.app.web.service.UserTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    private final ChatClient chatClient;

    private final UserTools userTools;

    @Autowired
    private IUserService userService;

    @GetMapping("/ai")
    @Operation(summary = "第二阶段 对话")
    public String test(String userId) {
        User user = userService.getById(1L);
        return chatClient.prompt()
                .user("你是一名高级风控专家，主要分析用户提现时的风险，包括是否IP变更，是否修改密码等等，全平台一共有5级，等级越高代表在平台资产和权益越丰富," +
                        "回答一定要简洁，比如能或者不能提币，并且进行简单说明一下，返回的内容就是json字符串，用户userId:"+userId)
                .tools(userTools)
                .call()
                .content();
    }

    @PostMapping("/chat")
    @Operation(summary = "OpenAI 对话")
    public Map<String, Object> chat(@Valid @RequestBody AiChatReq req) {
        return aiChatService.chat(req.getMessage());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "DeepSeek 流式对话")
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody AiChatReq req,
                                                HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        Flux<ServerSentEvent<String>> content = aiChatService.stream(req.getMessage())
                .map(text -> ServerSentEvent.builder(text).event("message").build());
        Flux<ServerSentEvent<String>> done = Flux.just(
                ServerSentEvent.builder("[DONE]").event("done").build()
        );
        return content.concatWith(done);
    }

}
