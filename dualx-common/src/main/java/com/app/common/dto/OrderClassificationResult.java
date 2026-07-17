package com.app.common.dto;

import lombok.Data;

/**
 * 订单分类结果DTO
 */
@Data
public class OrderClassificationResult {
    
    private final boolean success;
    private final boolean largeOrder;

    private OrderClassificationResult(boolean success, boolean largeOrder) {
        this.success = success;
        this.largeOrder = largeOrder;
    }

    public static OrderClassificationResult success(boolean isLargeOrder) {
        return new OrderClassificationResult(true, isLargeOrder);
    }

    public static OrderClassificationResult fail() {
        return new OrderClassificationResult(false, false);
    }
}