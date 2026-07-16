package com.app.web.service;

import com.app.common.enums.WalletLogTypeEnum;
import com.app.common.operation.WalletOperation;
import com.app.db.entity.UserWallet;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 资产 服务类
 * </p>
 *
 * @author HayDen
 * @since 2024-06-24
 */
public interface IUserWalletService extends IService<UserWallet> {

    /**
     * 初始化资产表
     * @param memberId
     * @param address
     */
    void initWallet(int memberId, String address);

    /**
     * 根据钱包ID查询资产
     * @param walletId
     * @return
     */
    UserWallet getWallet(int walletId);

    /**
     * 根据用户ID和币种查询资产
     * @param memberId
     * @param coinName
     * @return
     */
    UserWallet getWallet(Integer memberId, String coinName);

    /**
     * 操作资产余额
     * @param op
     * @return
     */
    boolean operateBalance(WalletOperation op);

    /**
     * 操作资产余额
     * @param walletId
     * @param logType
     * @param opValue
     * @param extRemark
     * @return
     */
    boolean operateBalance(Integer walletId, WalletLogTypeEnum logType, BigDecimal opValue, String extRemark);

    /**
     * 操作资产冻结
     * @param op
     * @return
     */
    boolean operateFrozen(WalletOperation op);

    /**
     * 操作资产冻结
     * @param walletId
     * @param logType
     * @param opValue
     * @param extRemark
     * @return
     */
    boolean operateFrozen(Integer walletId, WalletLogTypeEnum logType, BigDecimal opValue, String extRemark);

    /**
     * 批量操作资产
     * @param operations
     * @return
     */
    boolean batchOperate(List<WalletOperation> operations);

    /**
     * 批量操作资产
     * @param operations
     * @param check
     * @return
     */
    boolean batchOperate(List<WalletOperation> operations, boolean check);

    /**
     * 批量操作资产
     * @param operations
     * @param check
     * @param sort
     * @return
     */
    boolean batchOperate(List<WalletOperation> operations, boolean check, boolean sort);
}
