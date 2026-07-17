package com.app.web.api;

import com.app.web.service.IBscWithdrawalLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 根据id查询一条数据
     *
     * @return map对象
     */
    @GetMapping("/{orderId}")
    @Operation(summary ="根据ID查询提现记录表")
    public Map<String, String> findOne(@PathVariable String orderId) {
        log.info("收到查询请求ID为{}", orderId);
        return bscWithdrawalLogService.getWithdrawalStatusByOrderId(orderId);
    }



}
 
