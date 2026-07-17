package com.app.common.constants;

/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-20 9:31
 */
public interface RedisConstants {

    /**
     * 代币汇率缓存Key前缀
     */
    String TOKEN_PRICE_CACHE_PREFIX = "token:price:";

    /**
     * DUON汇率缓存前缀
     */
    String DUON_PRICE_CACHE_PREFIX = "duon:price:";

    /**
     * 代币汇率更新锁Key前缀
     */
    String PRICE_CACHE_PREFIX_DEAL = "token:price:deal:";

    /**
     * 起始区块key
     */
    String REDIS_KEY_SCANNED_BLOCK = "BSC_CHAIN_EVENT:SCANNED_BLOCK";

    /**
     * 结束区块
     */
    String REDIS_KEY_LATEST_BLOCK = "BSC_CHAIN_EVENT:LATEST_BLOCK";

    /**
     * 提现订单防重key
     */
    String EXCHANGE_GRADE_WITHDRAW_SEND = "EXCHANGE_GRADE_WITHDRAW_SEND:";

    /**
     * 资金池余额不足
     */
    String WITHDRAW_BALANCE_EXECUTE = "WITHDRAW:BALANCE:EXECUTE:";

    /**
     * 支付交易哈希查询限流 Redis Key 前缀。
     * <p>
     * 限制规则：
     * 同一个合约地址 + 同一个交易哈希，3 秒内只能查询一次。
     * <p>
     * Redis Key 示例：
     * payment:hash:query:limit:0x合约地址:0x交易哈希
     */
    String PAYMENT_HASH_QUERY_LIMIT_PREFIX = "payment:hash:query:limit:";

    String PAYMENT_HASH = "payment:hash";

}
