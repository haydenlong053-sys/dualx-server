package com.app.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DualxApplication {

    public static void main(String[] args) {
       SpringApplication.run(DualxApplication.class, args);
       log.info("dualx-api 项目启动完成");
    }


}
