package com.app.web.service;

import com.app.db.entity.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-20 10:41
 */
public interface ISysConfigService extends IService<SysConfig> {

    String getConfigValueByKey(String key);

    boolean updateConfigValueByKey(String key,String value);
}
