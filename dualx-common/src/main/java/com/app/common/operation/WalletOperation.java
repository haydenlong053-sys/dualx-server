package com.app.common.operation;

import com.app.common.enums.WalletLogTypeEnum;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletOperation {

    //类型: 1仅余额，2仅冻结
    private int type;
    private int walletId;
    private WalletLogTypeEnum opType;
    private BigDecimal opAmount;
    private String extRemark;

    private WalletOperation() {
    }

    public static WalletOperation buildParam(int type, int walletId,
                                             WalletLogTypeEnum opType,
                                             BigDecimal opAmount,
                                             String extRemark) {
        WalletOperation dto = new WalletOperation();
        dto.type = type;
        dto.walletId = walletId;
        dto.opType = opType;
        dto.opAmount = opAmount;
        dto.extRemark = extRemark;
        return dto;
    }
}
