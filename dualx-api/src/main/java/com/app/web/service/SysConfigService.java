package com.app.web.service;

import com.app.db.entity.SysConfig;

import java.math.BigDecimal;

public interface SysConfigService {

    BigDecimal getNumValue(String key, BigDecimal defaultValue);

    BigDecimal getNumValue(String key);

    BigDecimal getPercentValue(String key);

    String getStringValue(String key);

    Object getObjectValue(String key, Class<?> targetClass);

    Integer getIntValue(String key);

    SysConfig getEntityWithCache(String key);

    void update(String key, String value);
}
