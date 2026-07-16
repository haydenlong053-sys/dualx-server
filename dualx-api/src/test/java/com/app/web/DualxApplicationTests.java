package com.app.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = DualxApplication.class)
public class DualxApplicationTests {

    @BeforeEach
    public void init() {
        //System.out.println("开始测试-----------------");
    }

    @AfterEach
    public void after() {
        //System.out.println("测试结束-----------------");
    }
}
