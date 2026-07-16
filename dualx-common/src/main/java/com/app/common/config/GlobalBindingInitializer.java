package com.app.common.config;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.WebDataBinder;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@ControllerAdvice
public class GlobalBindingInitializer {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDateTime.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), true) {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                // 将传入的日期时间字符串转换为 LocalDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                // 转换为 UTC 时间
                LocalDateTime utcDateTime = localDateTime.atOffset(ZoneOffset.UTC).toLocalDateTime();
                setValue(utcDateTime);
            }
        });

        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                try {
                    // 将传入的日期时间字符串转换为 java.util.Date
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text);
                    // 转换为 UTC 时间
                    Date utcDate = Date.from(date.toInstant().atOffset(ZoneOffset.UTC).toInstant());
                    setValue(utcDate);
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid date format", e);
                }
            }
        });
    }
}
