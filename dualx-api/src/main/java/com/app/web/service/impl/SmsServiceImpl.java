package com.app.web.service.impl;

import com.app.common.enums.SmsStatusEnum;
import com.app.web.msg.EmailSingleSender;
import com.app.web.msg.EmailSingleSenderResult;
import com.app.web.service.SmsService;
import com.app.web.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Resource
    private SysConfigService sysConfigService;

    @Override
    public boolean sendSmsEmailMordhuyun(String mail, SmsStatusEnum smsStatusEnum, String code) {
        if (StringUtils.isBlank(mail) || smsStatusEnum == null) {
            return false;
        }
        String accessKey = sysConfigService.getStringValue("EMAIL_ACCESS_KEY");
        String secretKey = sysConfigService.getStringValue("EMAIL_SECRET_KEY");
        String fromEmail = sysConfigService.getStringValue("EMAIL_FROM");
        if (StringUtils.isAnyBlank(accessKey, secretKey, fromEmail)) {
            log.warn("email config missing, skip send to {}", mail);
            return true;
        }
        try {
            EmailSingleSender sender = new EmailSingleSender(accessKey, secretKey);
            String body = String.format(smsStatusEnum.getMsg(), code);
            String subject = smsStatusEnum.getTitle() != null ? smsStatusEnum.getTitle() : "Verification";
            EmailSingleSenderResult result = sender.send(
                0,
                fromEmail,
                mail,
                "RWA",
                false,
                fromEmail,
                subject,
                body,
                "0",
                ""
            );
            return result != null && result.result == 0;
        } catch (Exception e) {
            log.error("send email failed, mail={}", mail, e);
            return false;
        }
    }
}
