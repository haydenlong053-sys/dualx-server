package com.app.web.service;

import com.app.common.enums.SmsStatusEnum;

public interface SmsService {

    boolean sendSmsEmailMordhuyun(String mail, SmsStatusEnum smsStatusEnum, String code);
}
