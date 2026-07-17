package com.app.common.enums;

import lombok.Getter;

@Getter
public enum CoinEnum {

    USDT(1, "USDT"),
    OIDC(2, "OIDC");

    private final Integer coinId;
    private final String coinName;

    CoinEnum(Integer coinId, String coinName) {
        this.coinId = coinId;
        this.coinName = coinName;
    }

    public static CoinEnum getByCoinId(Integer coinId) {
        if (coinId == null) {
            return null;
        }

        for (CoinEnum item : values()) {
            if (item.getCoinId().equals(coinId)) {
                return item;
            }
        }

        return null;
    }

    public static boolean isUsdt(Integer coinId) {
        return USDT.getCoinId().equals(coinId);
    }

    public static boolean isOidc(Integer coinId) {
        return OIDC.getCoinId().equals(coinId);
    }
}