package com.app.web.service;

import com.app.db.entity.UserWalletLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserWalletLogService extends IService<UserWalletLog> {

    boolean saveCoinLog(UserWalletLog walletLog);
}
