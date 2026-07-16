package com.app.web.service.impl;

import com.app.common.util.JsonUtil;
import com.app.common.util.RedisUtil;
import com.app.db.entity.SysConfig;
import com.app.db.mapper.SysConfigMapper;
import com.app.web.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service
public class SysConfigServiceImpl implements SysConfigService {

    @Resource
    private SysConfigMapper sysConfigMapper;

    @Override
    public BigDecimal getNumValue(String key, BigDecimal defaultValue) {
        BigDecimal result = getNumValue(key);
        return result != null ? result : defaultValue;
    }

    @Override
    public BigDecimal getNumValue(String key) {
        SysConfig entity = getEntityWithCache(key);
        if (entity == null || entity.getConfigValue() == null || entity.getConfigValue().isEmpty()) {
            return null;
        }
        return new BigDecimal(entity.getConfigValue());
    }

    @Override
    public BigDecimal getPercentValue(String key) {
        SysConfig entity = getEntityWithCache(key);
        if (entity == null) {
            return null;
        }
        return new BigDecimal(entity.getConfigValue());
    }

    @Override
    public String getStringValue(String key) {
        SysConfig entity = getEntityWithCache(key);
        if (entity == null) {
            return null;
        }
        return entity.getConfigValue();
    }

    @Override
    public Object getObjectValue(String key, Class<?> targetClass) {
        SysConfig entity = getEntityWithCache(key);
        if (entity == null) {
            return null;
        }
        return JsonUtil.fromJson(entity.getConfigValue(), targetClass);
    }

    @Override
    public Integer getIntValue(String key) {
        SysConfig entity = getEntityWithCache(key);
        if (entity == null || entity.getConfigValue() == null) {
            return null;
        }
        return Integer.parseInt(entity.getConfigValue());
    }

    @Override
    public SysConfig getEntityWithCache(String key) {
        String cacheKey = "ids:sysConfig:" + key;
        String cacheValue = RedisUtil.get(cacheKey);
        if (cacheValue != null) {
            return JsonUtil.fromJson(cacheValue, SysConfig.class);
        }
        SysConfig entity = sysConfigMapper.selectOne(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key).last("limit 1")
        );
        RedisUtil.setEx(cacheKey, JsonUtil.toJson(entity), 5);
        return entity;
    }

    @Override
    public void update(String key, String value) {
        sysConfigMapper.update(null,
            new LambdaUpdateWrapper<SysConfig>()
                .set(SysConfig::getConfigValue, value)
                .eq(SysConfig::getConfigKey, key)
        );
    }
}
