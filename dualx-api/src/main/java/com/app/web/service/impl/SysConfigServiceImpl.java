package com.app.web.service.impl;

import com.app.db.entity.SysConfig;
import com.app.db.mapper.SysConfigMapper;
import com.app.web.service.ISysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-20 10:44
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Override
    public String getConfigValueByKey(String key) {
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey, key);
        queryWrapper.last("limit 1");
        SysConfig sysConfig = baseMapper.selectOne(queryWrapper);
        if (Objects.nonNull(sysConfig)) {
            return sysConfig.getConfigValue();
        }
        return null;
    }


    @Override
    public boolean updateConfigValueByKey(String key, String value) {
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey, key);
        queryWrapper.last("limit 1");

        SysConfig sysConfig = baseMapper.selectOne(queryWrapper);
        if (Objects.isNull(sysConfig)) {
            return false;
        }

        sysConfig.setConfigValue(value);
        sysConfig.setUpdateTime(LocalDateTime.now());

        return baseMapper.updateById(sysConfig) > 0;
    }

}
