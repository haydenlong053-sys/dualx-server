package com.app.web;
 
import com.app.db.entity.User;
import com.app.web.service.IUserService;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Resource;

public class WebTest extends DualxApplicationTests {
 
 
    @Resource
    private IUserService userService;

    @Test
    public void testGetEntFileById(){
        User userRecord = userService.fetchOne(1);
        System.out.printf("测试类配置成功啦");
    }
 
    @Test
    public void testGetEntFileList(){
        System.out.printf("222222");
    }
}