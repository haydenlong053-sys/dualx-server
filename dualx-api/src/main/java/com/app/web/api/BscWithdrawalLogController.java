package com.app.web.api;

import com.app.web.service.IBscWithdrawalLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 提现记录表 前端控制器
 * </p>
 *
 * @author oyp
 * @since 2026-04-17
 */
@Slf4j
@RestController
@RequestMapping("/api/bsc-withdrawal-log")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BscWithdrawalLogController {

    @Resource
    private IBscWithdrawalLogService bscWithdrawalLogService;

    /**
     * 根据订单号和合约地址查询提现记录
     *
     * @param orderId        订单号
     * @param contractAddress 代币合约地址
     * @return 提现状态
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "根据订单号查询提现记录")
    public Map<String, String> findOne(
            @PathVariable("orderId") String orderId,
            @RequestParam("contractAddress") String contractAddress) {

        log.info("收到提现查询请求，orderId：{}，contractAddress：{}", orderId, contractAddress);
        return bscWithdrawalLogService.getWithdrawalStatusByOrderId(orderId, contractAddress);
    }


}
 
