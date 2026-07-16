package com.app.db.mapper;

import com.app.db.entity.UserWalletLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserWalletLogMapper extends BaseMapper<UserWalletLog> {

    int insertCoinLog(@Param("tableName") String tableName, @Param("log") UserWalletLog log);
}
