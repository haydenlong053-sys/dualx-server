package com.app.web.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.app.common.dto.PaymentOrderLog;
import com.app.common.dto.RechargeOrderLogDTO;
import com.app.common.dto.WithdrawOrderMessage;
import com.app.common.model.BaseResult;
import com.app.common.util.RedisUtil;
import com.app.web.service.IPaymentReconcileLogService;
import com.app.web.service.IRechargeReconcileLogService;
import com.app.web.service.IWithdrawReconcileLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/reconcile")
public class ReconcileDataController {

    @Resource
    private IPaymentReconcileLogService paymentReconcileLogService;

    @Resource
    private IWithdrawReconcileLogService withdrawReconcileLogService;

    @Resource
    private IRechargeReconcileLogService rechargeReconcileLogService;

    /**
     * 统一接收对账数据
     *
     * @param type 类型: payment-支付 recharge-充值 withdraw-提现
     * @param data 对应的DTO数据
     */
    @PostMapping("/sync/{type}")
    @Operation(summary = "统一同步对账数据")
    public BaseResult<?> syncReconcileData(@PathVariable String type, @RequestBody String data) {
        log.info("收到对账数据同步请求, type={}, data={}", type, data);
        try {
            switch (type.toLowerCase()) {
                case "payment":
                    PaymentOrderLog paymentLog = JSON.parseObject(data, PaymentOrderLog.class);
                    paymentReconcileLogService.saveBizOrder(paymentLog);
                    break;
                case "recharge":
                    RechargeOrderLogDTO rechargeLog = JSON.parseObject(data, RechargeOrderLogDTO.class);
                    rechargeReconcileLogService.saveBizOrder(rechargeLog);
                    break;
                case "withdraw":
                    WithdrawOrderMessage withdrawLog = JSON.parseObject(data, WithdrawOrderMessage.class);
                    withdrawReconcileLogService.saveOrUpdateBizOrder(withdrawLog);
                    break;
                default:
                    return BaseResult.error("不支持的type: " + type);
            }
            log.info("对账数据同步成功, type={}", type);
            return BaseResult.success("同步成功");
        } catch (Exception e) {
            log.error("对账数据同步失败, type={}", type, e);
            return BaseResult.error("同步失败: " + e.getMessage());
        }
    }

    @GetMapping("/getBalance")
    public BaseResult<?> getTokenPrice() {
        String value = RedisUtil.get("wallet:balance:monitor");
        JSONObject jsonObject = JSON.parseObject(value);
        return BaseResult.success("获取成功", jsonObject);
    }


}