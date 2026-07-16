package com.app.web.service.impl;

import com.app.db.entity.UserWalletLog;
import com.app.db.mapper.UserWalletLogMapper;
import com.app.web.service.IUserWalletLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserWalletLogServiceImpl extends ServiceImpl<UserWalletLogMapper, UserWalletLog> implements IUserWalletLogService {

    @Override
    public boolean saveCoinLog(UserWalletLog walletLog) {
        String tableName = coinLogTableName(walletLog.getCoinName());
        return baseMapper.insertCoinLog(tableName, walletLog) == 1;
    }

    private String coinLogTableName(String coinName) {
        if (StringUtils.isBlank(coinName)) {
            throw new IllegalArgumentException("币种不能为空");
        }
        String normalized = coinName.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{1,32}")) {
            throw new IllegalArgumentException("币种格式错误");
        }
        return "user_wallet_" + normalized + "_log";
    }
}
