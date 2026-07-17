package com.app.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.app.common.constants.RedisConstants;
import com.app.common.constants.SysConfigConstants;
import com.app.common.util.RedisUtil;
import com.app.common.util.TelegramAlert;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IAccessControlService;
import com.app.web.service.IGasCheckService;
import com.app.web.service.ISysConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Date;

@Service
@Slf4j
public class GasCheckServiceImpl implements IGasCheckService {


    @Resource
    private Web3j web3j;

    @Resource
    private IAccessControlService accessControlService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    private volatile long lastCheckTimeMillis = 0L;

    private volatile boolean lastCheckResult = true;

    @Override
    public void gas() {
        try {
            getTokenPrice();
        } catch (Exception e) {
            log.error("Gas检查任务执行异常", e);
        }
    }

    @Override
    public void getTokenPrice() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 1L);

        BigDecimal bnbMinBalance = new BigDecimal("0.001");
        BigDecimal odicMinBalance = new BigDecimal(sysConfigService.getConfigValueByKey("ODIC_MIN_BALANCE"));
        BigDecimal duonMinBalance = new BigDecimal(sysConfigService.getConfigValueByKey("DUON_MIN_BALANCE"));
        BigDecimal usdtMinBalance = new BigDecimal(sysConfigService.getConfigValueByKey("USDT_MIN_BALANCE"));
        String alertUsU = sysConfigService.getConfigValueByKey("ALERT_US_USDT");
        String alertUsOdic = sysConfigService.getConfigValueByKey("ALERT_US_ODIC");
        String alertProductUsdt = sysConfigService.getConfigValueByKey("ALERT_PRODUCT_USDT");
        String alertProductOdic = sysConfigService.getConfigValueByKey("ALERT_PRODUCT_ODIC");


        // 执行钱包 BNB 余额
        BigDecimal executeWalletBnbBalance = weiToEth(getNativeBalance(withdrawContractConfig.getExecuteWithdrawAddress()));
        boolean executeWalletBnbEnough = executeWalletBnbBalance.compareTo(bnbMinBalance) >= 0;
        jsonObject.put("executeWalletBnbBalance", executeWalletBnbBalance);
        jsonObject.put("executeWalletBnbMinBalance", bnbMinBalance);
        jsonObject.put("executeWalletBnbEnough", executeWalletBnbEnough);

        // 添加白名单钱包 BNB 余额
        BigDecimal whitelistWalletBnbBalance = weiToEth(getNativeBalance(withdrawContractConfig.getExecuteWithdrawAddress()));
        boolean whitelistWalletBnbEnough = whitelistWalletBnbBalance.compareTo(bnbMinBalance) >= 0;
        jsonObject.put("whitelistWalletBnbBalance", whitelistWalletBnbBalance);
        jsonObject.put("whitelistWalletBnbMinBalance", bnbMinBalance);
        jsonObject.put("whitelistWalletBnbEnough", whitelistWalletBnbEnough);

        // 闪兑提币 USDT 资金池，目前没有这个提现方式
        BigDecimal swapPoolUsdtBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ONE, withdrawContractConfig.getContractWithdrawUsdt()));
        boolean swapPoolUsdtEnough = true;
        jsonObject.put("swapPoolUsdtBalance", swapPoolUsdtBalance);
        jsonObject.put("swapPoolUsdtMinBalance", new BigDecimal("0"));
        jsonObject.put("swapPoolUsdtEnough", swapPoolUsdtEnough);

        // 正常提币 USDT 余额
        BigDecimal withdrawPoolUsdtBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ZERO, withdrawContractConfig.getContractWithdrawUsdt()));
        boolean withdrawPoolUsdtEnough = withdrawPoolUsdtBalance.compareTo(usdtMinBalance) >= 0;
        jsonObject.put("withdrawPoolUsdtBalance", withdrawPoolUsdtBalance);
        jsonObject.put("withdrawPoolUsdtMinBalance", usdtMinBalance);
        jsonObject.put("withdrawPoolUsdtEnough", withdrawPoolUsdtEnough);

        // 闪兑提币 ODIC
        BigDecimal swapPoolOdicBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ONE, withdrawContractConfig.getContractWithdrawOdic()));
        boolean swapPoolOdicEnough = swapPoolOdicBalance.compareTo(odicMinBalance) >= 0;
        jsonObject.put("swapPoolOdicBalance", swapPoolOdicBalance);
        jsonObject.put("swapPoolOdicMinBalance", odicMinBalance);
        jsonObject.put("swapPoolOdicEnough", swapPoolOdicEnough);

        // 正常提币 ODIC
        BigDecimal withdrawPoolOdicBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ZERO, withdrawContractConfig.getContractWithdrawOdic()));
        boolean withdrawPoolOdicEnough = withdrawPoolOdicBalance.compareTo(odicMinBalance) >= 0;
        jsonObject.put("withdrawPoolOdicBalance", withdrawPoolOdicBalance);
        jsonObject.put("withdrawPoolOdicMinBalance", odicMinBalance);
        jsonObject.put("withdrawPoolOdicEnough", withdrawPoolOdicEnough);

        //美区USDT余额
        BigDecimal usUsdtBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ZERO, withdrawContractConfig.getUsContractWithdrawUsdt()));
        boolean usUsdtEnough = usUsdtBalance.compareTo(usdtMinBalance) >= 0;
        jsonObject.put("usUsdtBalance", usUsdtBalance);
        jsonObject.put("usUsdtMinBalance", usdtMinBalance);
        jsonObject.put("usUsdtEnough", usUsdtEnough);

        //美区ODIC余额
        BigDecimal usOdicBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ZERO, withdrawContractConfig.getUsContractWithdrawOdic()));
        boolean usOdicEnough = usOdicBalance.compareTo(odicMinBalance) >= 0;
        jsonObject.put("usOdicBalance", usOdicBalance);
        jsonObject.put("usOdicMinBalance", odicMinBalance);
        jsonObject.put("usOdicEnough", usOdicEnough);


        //product区域USDT余额
        BigDecimal productUsdtBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ZERO, withdrawContractConfig.getProductContractWithdrawUsdt()));
        boolean productUsdtEnough = productUsdtBalance.compareTo(usdtMinBalance) >= 0;
        jsonObject.put("productUsdtBalance", productUsdtBalance);
        jsonObject.put("productUsdtMinBalance", usdtMinBalance);
        jsonObject.put("productUsdtEnough", productUsdtEnough);

        //product区域DUON余额
        BigDecimal productOdicBalance = weiToEth(accessControlService.getTreasuryBalance(BigInteger.ZERO, withdrawContractConfig.getProductContractWithdrawOdic()));
        boolean productOdicEnough = productOdicBalance.compareTo(duonMinBalance) >= 0;
        jsonObject.put("productOdicBalance", productOdicBalance);
        jsonObject.put("productOdicMinBalance", duonMinBalance);
        jsonObject.put("productOdicEnough", productOdicEnough);


        boolean allEnough = executeWalletBnbEnough
                && whitelistWalletBnbEnough
                && usOdicEnough
                && usUsdtEnough
                && productUsdtEnough
                && productOdicEnough;
        jsonObject.put("allEnough", allEnough);
        jsonObject.put("lastCheckTime", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        RedisUtil.setEx("wallet:balance:monitor", jsonObject.toString(), 60);

        // ========== 发送余额不足告警 ==========
        StringBuilder alertContent = new StringBuilder();

        if (!executeWalletBnbEnough) {
            alertContent.append("提现执行钱包\n")
                    .append("地址：").append(withdrawContractConfig.getExecuteWithdrawAddress()).append("\n")
                    .append("币种：BNB\n")
                    .append("余额：").append(executeWalletBnbBalance.stripTrailingZeros().toPlainString()).append(" BNB\n\n");
        }

        if (!whitelistWalletBnbEnough) {
            alertContent.append("白名单钱包\n")
                    .append("地址：").append(withdrawContractConfig.getAddWhiteAddress()).append("\n")
                    .append("币种：BNB\n")
                    .append("余额：").append(whitelistWalletBnbBalance.stripTrailingZeros().toPlainString()).append(" BNB\n\n");
        }

        if (!swapPoolUsdtEnough) {
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "USDT", alertContent.toString(), 600);
        }

        if (!withdrawPoolUsdtEnough) {
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "USDT", alertContent.toString(), 600);
        }


        if (!swapPoolOdicEnough) {
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "ODIC", alertContent.toString(), 600);
        }

        if (!withdrawPoolOdicEnough) {
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "ODIC", alertContent.toString(), 600);
        }

        if (!usUsdtEnough) {
            if (!"0".equals(alertUsU)) {
                alertContent.append("美区——提现池\n")
                        .append("地址：").append("0x9c9DA5c070662e483E598978dBf01c2c068dbd66").append("\n")
                        .append("币种：USDT\n")
                        .append("余额：").append(usUsdtBalance.stripTrailingZeros().toPlainString()).append(" USDT\n\n");
            }
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "USDT_US", alertContent.toString(), 600);
        }

        if (!usOdicEnough) {
            if (!"0".equals(alertUsOdic)) {
                alertContent.append("美区——提现池\n")
                        .append("地址：").append("0x9c9DA5c070662e483E598978dBf01c2c068dbd66").append("\n")
                        .append("币种：ODIC\n")
                        .append("余额：").append(usOdicBalance.stripTrailingZeros().toPlainString()).append(" ODIC\n\n");
            }
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "ODIC_US", alertContent.toString(), 600);
        }

        if (!productUsdtEnough) {
            if (!"0".equals(alertProductUsdt)) {
                alertContent.append("PRODUCT区——提现池\n")
                        .append("地址：").append("0x3835500704d2dDBf6861AA1A4fE130B3056A91Fa").append("\n")
                        .append("币种：USDT\n")
                        .append("余额：").append(productUsdtBalance.stripTrailingZeros().toPlainString()).append(" USDT\n\n");
            }
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "USDT_PRODUCT", alertContent.toString(), 600);
        }

        if (!productOdicEnough) {
            if (!"0".equals(alertProductOdic)) {
                alertContent.append("PRODUCT区——提现池\n")
                        .append("地址：").append("0x3835500704d2dDBf6861AA1A4fE130B3056A91Fa").append("\n")
                        .append("币种：DUON\n")
                        .append("余额：").append(productOdicBalance.stripTrailingZeros().toPlainString()).append(" ODIC\n\n");
            }
            RedisUtil.setEx(RedisConstants.WITHDRAW_BALANCE_EXECUTE + "ODIC_PRODUCT", alertContent.toString(), 600);
        }

        // 最后统一发送
        if (alertContent.length() > 0) {
            String botToken = sysConfigService.getConfigValueByKey("BOT_TOKEN_ENV");
            String chatId = sysConfigService.getConfigValueByKey("CHAT_ID_ENV");
            TelegramAlert.sendAlert("余额不足告警", alertContent.toString(), botToken, chatId);
        }

    }

    @Override
    public BigDecimal weiToEth(BigInteger wei) {
        if (wei == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(wei).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.DOWN);
    }

    @Override
    public boolean hasEnoughGas(String executorAddress) {
        long now = System.currentTimeMillis();

        // 缓存判断
        if (now - lastCheckTimeMillis < SysConfigConstants.GAS_CHECK_CACHE_SECONDS * 1000) {
            log.debug("使用缓存的Gas检查结果, enough={}", lastCheckResult);
            return lastCheckResult;
        }

        synchronized (this) {
            // 双重检查
            now = System.currentTimeMillis();
            if (now - lastCheckTimeMillis < SysConfigConstants.GAS_CHECK_CACHE_SECONDS * 1000) {
                return lastCheckResult;
            }

            try {
                BigInteger balance = getNativeBalance(executorAddress);
                lastCheckResult = balance != null && balance.compareTo(SysConfigConstants.MIN_GAS_BALANCE) >= 0;
                lastCheckTimeMillis = now;

                if (!lastCheckResult) {
                    log.error("执行钱包Gas余额不足, executor={}, balanceWei={}, minRequiredWei={}", executorAddress, balance, SysConfigConstants.MIN_GAS_BALANCE);
                } else {
                    log.debug("执行钱包Gas余额充足, balanceWei={}", balance);
                }

                return lastCheckResult;

            } catch (Exception e) {
                log.error("检查执行钱包Gas余额异常", e);
                lastCheckResult = false;
                lastCheckTimeMillis = now;
                return false;
            }
        }
    }

    @Override
    public BigInteger getNativeBalance(String address) throws IOException {
        EthGetBalance ethGetBalance = web3j
                .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send();

        if (ethGetBalance.hasError()) {
            log.error("查询余额失败, address={}, error={}", address, ethGetBalance.getError());
            return BigInteger.ZERO;
        }

        return ethGetBalance.getBalance();
    }
}