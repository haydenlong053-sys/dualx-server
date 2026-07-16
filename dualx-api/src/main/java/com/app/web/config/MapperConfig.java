package com.app.web.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 程序注解配置
 *
 * @author HayDen
 */
@Configuration
// 指定要扫描的Mapper类的包的路径
@MapperScan(value = "com.app.db.mapper")
public class MapperConfig
{

}
