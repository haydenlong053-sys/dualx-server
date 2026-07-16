package com.app.web.service.impl;

import com.app.common.enums.WalletLogTypeEnum;
import com.app.common.operation.WalletOperation;
import com.app.common.util.CoinUtil;
import com.app.db.entity.UserWallet;
import com.app.db.entity.UserWalletLog;
import com.app.db.mapper.UserWalletMapper;
import com.app.web.service.IUserWalletLogService;
import com.app.web.service.IUserWalletService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * 资产 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2024-06-24
 */
@Slf4j
@Service
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet> implements IUserWalletService {

    private static final int WALLET_UPDATE_RETRY_TIMES = 3;

    @Resource
    private IUserWalletLogService userWalletLogService;

    @Value(value = "${spring.profiles.active}")
    private String env;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initWallet(int memberId,String address) {
        Set<String> coinNames = CoinUtil.getCoinMap().keySet();
        for (String coinName : coinNames) {
            UserWallet walletRecord = new UserWallet();
            walletRecord.setCoinName(coinName);
            walletRecord.setMemberId(memberId);
            this.save(walletRecord);
            if (!env.equals("prod")) {
                BigDecimal gift = BigDecimal.valueOf(20000L);
                operateBalance(walletRecord.getId(), WalletLogTypeEnum.ADMIN_ADD, gift, null);
            }
        }
    }

    @Override
    public UserWallet getWallet(int walletId) {
        return this.getById(walletId);
    }

    @Override
    public UserWallet getWallet(Integer memberId, String coinName) {
        if (memberId == null || StringUtils.isBlank(coinName)) {
            return null;
        }
        LambdaQueryWrapper<UserWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserWallet::getMemberId, memberId).eq(UserWallet::getCoinName, coinName);
        UserWallet walletRecord = this.getOne(wrapper);
        if(walletRecord == null){
            walletRecord = new UserWallet();
            walletRecord.setCoinName(coinName);
            walletRecord.setMemberId(memberId);
            this.save(walletRecord);
        }
        return walletRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean operateBalance(WalletOperation op) {
        return operateBalance(op.getWalletId(), op.getOpType(), op.getOpAmount(), op.getExtRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean operateBalance(Integer walletId, WalletLogTypeEnum logType, BigDecimal opValue, String extRemark) {
        return operateAmount(walletId, logType, opValue, extRemark, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean operateFrozen(WalletOperation op) {
        return operateFrozen(op.getWalletId(), op.getOpType(), op.getOpAmount(), op.getExtRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean operateFrozen(Integer walletId, WalletLogTypeEnum logType, BigDecimal opValue, String extRemark) {
        return operateAmount(walletId, logType, opValue, extRemark, true);
    }

    private boolean operateAmount(Integer walletId, WalletLogTypeEnum logType, BigDecimal opValue,
                                  String extRemark,boolean frozenOperate) {
        if (walletId == null || opValue == null) {
            return false;
        }
        if (opValue.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        String amountColumn = frozenOperate ? "frozen" : "balance";
        int logAmountType = frozenOperate ? 2 : 1;
        for (int i = 1; i <= WALLET_UPDATE_RETRY_TIMES; i++) {
            UserWallet walletRecord = getWallet(walletId);
            if (walletRecord == null) {
                return false;
            }

            BigDecimal before = frozenOperate ? walletRecord.getFrozen() : walletRecord.getBalance();
            before = before == null ? BigDecimal.ZERO : before;
            BigDecimal after = before.add(opValue);
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }

            UpdateWrapper<UserWallet> updateWrapper = new UpdateWrapper<>();
            updateWrapper
                .setSql(true, amountColumn + " = " + amountColumn + " + {0}, version = IFNULL(version, 1) + 1", opValue)
                .eq("id", walletId);
            if (walletRecord.getVersion() == null) {
                updateWrapper.isNull("version");
            } else {
                updateWrapper.eq("version", walletRecord.getVersion());
            }
            if (opValue.compareTo(BigDecimal.ZERO) < 0) {
                updateWrapper.ge(amountColumn, opValue.abs());
            }

            boolean updated = this.update(updateWrapper);
            if (updated) {
                saveWalletLog(walletRecord, logAmountType, logType, opValue, before, after, extRemark);
                return true;
            }
            log.warn("资产版本冲突，准备重试 walletId={}, column={}, retry={}", walletId, amountColumn, i);
        }
        log.warn("资产操作失败，超过最大重试次数 walletId={}, column={}, amount={}", walletId, amountColumn, opValue);
        return false;
    }

    private void saveWalletLog(UserWallet walletRecord, int amountType, WalletLogTypeEnum logType,
                               BigDecimal opValue, BigDecimal before, BigDecimal after, String extRemark) {
        UserWalletLog walletLog = new UserWalletLog();
        walletLog.setWalletId(walletRecord.getId());
        walletLog.setMemberId(walletRecord.getMemberId());
        walletLog.setType(amountType);
        walletLog.setCoinName(walletRecord.getCoinName());
        walletLog.setOpValue(opValue);
        walletLog.setOpBefore(before);
        walletLog.setOpAfter(after);
        walletLog.setOpType(logType.getCode());
        walletLog.setOpRemark(logType.getRemark());
        walletLog.setExtRemark(extRemark != null ? extRemark : "");
        if (!userWalletLogService.save(walletLog)) {
            throw new IllegalStateException("资产流水保存失败");
        }
        if (!userWalletLogService.saveCoinLog(walletLog)) {
            throw new IllegalStateException("币种资产流水保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperate(List<WalletOperation> operations) {
        return batchOperate(operations, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperate(List<WalletOperation> operations, boolean check) {
        return batchOperate(operations, check, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperate(List<WalletOperation> operations, boolean check, boolean sort) {
        if (sort) {
            operations.sort(Comparator.comparingInt(WalletOperation::getWalletId));
        }
        for (WalletOperation item : operations) {
            boolean res = false;
            switch (item.getType()) {
                case 1:
                    res = operateBalance(item);
                    break;
                case 2:
                    res = operateFrozen(item);
                    break;
                default:
                    log.warn("bad wallet operate type");
            }
            if (!res && check) {
                log.error("批量操作失败");
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
        }
        return true;
    }

}
