package com.app.common.constants;

import java.math.BigInteger;

/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-20 10:46
 */
public interface SysConfigConstants {


    Integer REDIS_ONE_WEEK_SECONDS = 7 * 24 * 60 * 60;
    Integer REDIS_ONE_DAY_SECONDS = 24 * 60 * 60;

    String WITHDRAW_EXECUTOR_ROLE = "0xa808b4b0557ce0b3bf870505e851955fc03e6e06acbe4aa20a6a8d3a196c7991";

    int TOKEN_DECIMALS = 18;

    /**
     * Topics 最小长度
     */
    int MIN_TOPICS_SIZE = 3;

    /**
     * Data 最小长度
     */
    int MIN_DATA_SIZE = 5;

    /**
     * 小额所需签名数
     */
    int SIGNATURE_THRESHOLD = 3;

    //最低手续费
    BigInteger MIN_GAS_BALANCE = new BigInteger("1000000000000000");
    /**
     * Gas检查缓存时间（秒）
     */
    long GAS_CHECK_CACHE_SECONDS = 60L;

    /**
     * 博饼交易地址
     */
    String PANCAKE_ROUTER = "0x10ED43C718714eb63d5aA57B78B54704E256024E";
}
