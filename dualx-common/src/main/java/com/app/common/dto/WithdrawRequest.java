package com.app.common.dto;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.math.BigInteger;

/**
 * 提现请求结构体
 *
 * 对应 Solidity:
 * struct WithdrawRequest {
 *     uint256 orderId;
 *     address user;
 *     uint256 amount;
 *     uint8 redemption;
 *     uint256 deadline;
 *     uint256 userNonce;
 *     uint256 bizId;
 * }
 */
public class WithdrawRequest extends StaticStruct {

    private BigInteger orderId;
    private String user;
    private BigInteger amount;
    private BigInteger redemption;
    private BigInteger deadline;
    /**
     * 币种ID
     */
    private BigInteger bizId;

    public WithdrawRequest(
            BigInteger orderId,
            String user,
            BigInteger amount,
            BigInteger redemption,
            BigInteger deadline,
            BigInteger bizId
    ) {
        super(
                new Uint256(orderId),
                new Address(user),
                new Uint256(amount),
                new Uint8(redemption),
                new Uint256(deadline),
                new Uint256(bizId)
        );

        this.orderId = orderId;
        this.user = user;
        this.amount = amount;
        this.redemption = redemption;
        this.deadline = deadline;
        this.bizId = bizId;
    }

    public BigInteger getOrderId() {
        return orderId;
    }

    public String getUser() {
        return user;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public BigInteger getRedemption() {
        return redemption;
    }

    public BigInteger getDeadline() {
        return deadline;
    }


    public BigInteger getBizId() {
        return bizId;
    }
}