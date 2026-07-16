package com.app.common.util;

import com.app.common.SpringContextHolder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class MessageUtil {

    private final static MessageSource messageSource = SpringContextHolder.getBean("messageSource", MessageSource.class);

    public static String get(String msgKey, Object... args) {
        try {
            //return messageSource.getMessage(msgKey, args, Locale.ENGLISH); zh_CN
            return messageSource.getMessage(msgKey, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getResultMsg(int code) {
        return get(String.format("resultCode.%d", code));
    }
}
