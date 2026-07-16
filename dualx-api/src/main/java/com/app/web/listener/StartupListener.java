package com.app.web.listener;

import com.app.common.util.RedisUtil;
import com.app.web.service.IUserWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
@Slf4j
public class StartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private IUserWalletService userWalletService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 在这里执行启动时需要执行的代码
        /*log.info("====开始恢复0802空投异常数据");

        if(RedisUtil.get("USER_WALLET_TOKEN_RECYCLE0802") == null || RedisUtil.getNumber("USER_WALLET_TOKEN_RECYCLE0802") < 1){
            userWalletService.recycle();
            int num = 0;
            RedisUtil.set("USER_WALLET_TOKEN_RECYCLE0802", String.valueOf(++num));
        }
        log.info("====恢复0802空投异常数据完成");*/
    }
}

