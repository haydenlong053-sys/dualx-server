package com.app.web.service;

import com.app.db.entity.User;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;
import org.springframework.stereotype.Component;

@Component
public class UserTools {

    @Autowired
    private IUserService userService;


    @Tool(description = "根据用户ID查询用户信息")
    public String getUserInfo(Long userId) {
        User user =userService.getById(userId);
        return """
                用户ID:%s
                等级:VIP3
                连续提现:5次
                """.formatted(user.getId());
    }


    @Tool(description = "获取用户总资产")
    public String getUserInfo(Long userId) {
        return """
                用户ID:%s
                USDT:3000
                连续提现:5次
                """.formatted(user.getId());
    }
}