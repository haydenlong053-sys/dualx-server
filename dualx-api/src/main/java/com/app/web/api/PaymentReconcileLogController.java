package com.app.web.api;

import com.alibaba.fastjson.JSONObject;
import com.app.common.model.BaseResult;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IMultiTokenPaymentService;
import com.app.web.service.IPaymentReconcileLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author oyp
 * @since 2026-05-12
 */
@Slf4j
@RestController
@RequestMapping("/api/withdrawal")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PaymentReconcileLogController {

    @Resource
    private IPaymentReconcileLogService paymentReconcileLogService;

    @Resource
    private IMultiTokenPaymentService iMultiTokenPaymentService;
    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    @GetMapping("/{orderId}")
    @Operation(summary = "根据ID查询")
    public JSONObject findOne(@PathVariable String orderId) {
        return paymentReconcileLogService.getWithdrawalByOrderId(orderId, withdrawContractConfig.getPaymentContractAddress());
    }

    @PostMapping("/getPayByOrdCtr")
    @Operation(summary = "根据交易哈希和合约地址查询支付信息")
    public JSONObject getPaymentByOrderAndContract(@RequestBody JSONObject params) {
        String orderId = params.getString("orderId");
        String contractAddress = params.getString("contractAddress");
        return paymentReconcileLogService.getWithdrawalByOrderId(orderId, contractAddress);
    }

    @PostMapping("/getPaymentByTxHash")
    @Operation(summary = "根据交易哈希和合约地址查询支付信息")
    public BaseResult<?> imFindByHash(@RequestBody JSONObject params) {
        String hashValue = params.getString("hash");
        String contractAddress = params.getString("contractAddress");
        return iMultiTokenPaymentService.getPaymentByTxHash(hashValue, contractAddress);
    }

    @GetMapping("/getOdicPrice")
    public BaseResult<?> getTokenPrice(@RequestParam("amount") BigDecimal amount,
                                       @RequestParam("status") Integer status) {
        return paymentReconcileLogService.getTokenPrice(amount, status, withdrawContractConfig.getOdicContract());
    }

    @GetMapping("/getDuonPrice")
    public BaseResult<?> getDuonPrice(@RequestParam("amount") BigDecimal amount,
                                      @RequestParam("status") Integer status) {
        return paymentReconcileLogService.getTokenPrice(amount, status, withdrawContractConfig.getDuonContract());
    }

}
 
