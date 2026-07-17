package com.app.web.service.kms;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * KMS 链上交易发送服务
 * <p>
 * 作用：
 * 1. 使用 AWS KMS 签 RawTransaction
 * 2. 发送 ethSendRawTransaction
 * 3. 等待交易回执
 * <p>
 * 注意：
 * 这个类用于调用合约方法，例如：
 * - executeWithdraw
 * - setAllowedUser
 * - batchSetAllowedUsers
 */
@Slf4j
public class KmsTransactionService {

    private static final BigInteger SECP256K1_N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    private static final BigInteger SECP256K1_HALF_N = SECP256K1_N.divide(BigInteger.valueOf(2));
    private final AtomicLong localNonce = new AtomicLong(-1);
    private final Object broadcastLock = new Object();


    private final Web3j web3j;
    private final KmsClient kmsClient;
    private final String kmsKeyId;
    private final BigInteger chainId;

    @Getter
    private final String fromAddress;

    public KmsTransactionService(Web3j web3j, String region, String kmsKeyId, BigInteger chainId) {
        this.web3j = web3j;
        this.kmsKeyId = kmsKeyId;
        this.chainId = chainId;

        this.kmsClient = KmsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.fromAddress = getSignerAddressFromKms();

        log.info("KMS交易服务初始化完成, kmsKeyId={}, fromAddress={}, chainId={}",
                kmsKeyId, fromAddress, chainId);
    }

    /**
     * 发送合约交易
     *
     * @param contractAddress 合约地址
     * @param encodedFunction FunctionEncoder.encode(function)
     * @param value           BNB value，一般传 BigInteger.ZERO
     */
    public TransactionReceipt sendContractTransaction(String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        try {
            String txHash = sendContractTransactionOnlyBroadcast(contractAddress, encodedFunction, value);
            PollingTransactionReceiptProcessor processor = new PollingTransactionReceiptProcessor(web3j, 3000, 40);
            TransactionReceipt receipt = processor.waitForTransactionReceipt(txHash);
            log.info("KMS交易回执, txHash={}, status={}, blockNumber={}", txHash, receipt.getStatus(), receipt.getBlockNumber());
            return receipt;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (needResetNonce(msg)) {
                resetLocalNonce(msg);
            }
            throw e;
        }
    }


    public String sendContractTransactionOnlyBroadcast(String contractAddress, String encodedFunction, BigInteger value) throws Exception {
        synchronized (broadcastLock) {
            BigInteger nonce = null;
            String localTxHash;
            String signedRawTx;
            try {
                BigInteger gasPrice = getGasPriceWithRetry();
                BigInteger gasLimit = estimateGas(contractAddress, encodedFunction, value);
                nonce = getNonce();
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce, gasPrice, gasLimit, contractAddress,
                        value == null ? BigInteger.ZERO : value, encodedFunction
                );
                signedRawTx = signRawTransaction(rawTransaction);
                localTxHash = calculateSignedTransactionHash(signedRawTx);
                log.info("准备广播KMS交易, from={}, to={}, nonce={}, txHash={}, gasPrice={}, gasLimit={}", fromAddress, contractAddress, nonce, localTxHash, gasPrice, gasLimit);
            } catch (Exception e) {
                if (nonce != null) resetLocalNonce("广播前异常: " + e.getMessage());
                log.error("KMS交易广播前异常, from={}, nonce={}, error={}", fromAddress, nonce, e.getMessage(), e);
                throw e;
            }
            EthSendTransaction response;
            try {
                response = web3j.ethSendRawTransaction(signedRawTx).send();
            } catch (Exception e) {
                log.error("KMS交易广播结果未知, from={}, nonce={}, txHash={}, error={}", fromAddress, nonce, localTxHash, e.getMessage(), e);
                throw new RuntimeException("BROADCAST_UNKNOWN: nonce=" + nonce + ", txHash=" + localTxHash, e);
            }
            if (response == null) {
                throw new RuntimeException("BROADCAST_UNKNOWN: response is null, nonce=" + nonce + ", txHash=" + localTxHash);
            }
            if (response.hasError()) {
                String message = response.getError() == null ? "unknown" : response.getError().getMessage();
                if (isAlreadyKnown(message)) {
                    log.warn("节点已存在该交易, nonce={}, txHash={}, message={}", nonce, localTxHash, message);
                    return localTxHash;
                }
                resetLocalNonce("节点拒绝交易: " + message);
                throw new RuntimeException("ethSendRawTransaction error: " + message);
            }
            String txHash = response.getTransactionHash();
            if (txHash == null || txHash.trim().isEmpty()) {
                throw new RuntimeException("BROADCAST_UNKNOWN: txHash is empty, nonce=" + nonce + ", localTxHash=" + localTxHash);
            }
            log.info("KMS交易广播成功, from={}, nonce={}, txHash={}", fromAddress, nonce, txHash);
            return txHash;
        }
    }


    /**
     * 关闭 KMS Client
     */
    public void close() {
        if (kmsClient != null) {
            kmsClient.close();
        }
    }


    private String calculateSignedTransactionHash(String signedRawTx) {
        return Numeric.toHexString(Hash.sha3(Numeric.hexStringToByteArray(signedRawTx)));
    }

    private boolean isAlreadyKnown(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("already known") || lower.contains("known transaction");
    }

    private BigInteger getGasPriceWithRetry() throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return getGasPrice();
            } catch (java.io.IOException e) {
                lastException = e;
                log.warn("获取gasPrice失败, attempt={}, error={}", attempt, e.getMessage());
                if (attempt < 3) Thread.sleep(500L * attempt);
            }
        }
        throw lastException;
    }


    private String signRawTransaction(RawTransaction rawTransaction) {
        byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction, chainId.longValue());
        byte[] txHash = Hash.sha3(encodedTransaction);

        BigInteger[] rs = signDigestByKms(txHash);

        BigInteger r = rs[0];
        BigInteger s = rs[1];

        /*
         * Ethereum 要求 s 使用 low-s。
         * AWS KMS 返回的 s 可能是 high-s。
         */
        if (s.compareTo(SECP256K1_HALF_N) > 0) {
            s = SECP256K1_N.subtract(s);
        }

        int recId = recoverRecId(txHash, r, s);

        /*
         * EIP-155:
         * v = recId + 35 + chainId * 2
         */
        BigInteger v = BigInteger.valueOf(recId)
                .add(BigInteger.valueOf(35))
                .add(chainId.multiply(BigInteger.valueOf(2)));

        Sign.SignatureData signatureData = new Sign.SignatureData(
                Numeric.toBytesPadded(v, 1),
                Numeric.toBytesPadded(r, 32),
                Numeric.toBytesPadded(s, 32)
        );

        byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signatureData);
        return Numeric.toHexString(signedMessage);
    }

    /**
     * 使用 KMS 签 digest
     */
    private BigInteger[] signDigestByKms(byte[] digest) {
        SignRequest request = SignRequest.builder()
                .keyId(kmsKeyId)
                .message(SdkBytes.fromByteBuffer(ByteBuffer.wrap(digest)))
                .messageType(MessageType.DIGEST)
                .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
                .build();

        SignResponse response = kmsClient.sign(request);
        return parseDerSignature(response.signature().asByteArray());
    }

    /**
     * 恢复 recId
     */
    private int recoverRecId(byte[] digest, BigInteger r, BigInteger s) {
        for (int recId = 0; recId < 4; recId++) {
            try {
                ECDSASignature signature = new ECDSASignature(r, s);
                BigInteger publicKey = Sign.recoverFromSignature(recId, signature, digest);

                if (publicKey != null) {
                    String recoveredAddress = "0x" + publicKeyToAddress(publicKey);

                    if (recoveredAddress.equalsIgnoreCase(fromAddress)) {
                        return recId;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        throw new RuntimeException("无法恢复recId, fromAddress=" + fromAddress);
    }

    /**
     * 获取 nonce
     */
    private BigInteger getNonce() throws Exception {
        EthGetTransactionCount count = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).send();

        if (count == null) throw new RuntimeException("获取nonce失败: response为空");
        if (count.hasError()) throw new RuntimeException("获取nonce失败: " + count.getError().getMessage());

        BigInteger chainNonce = count.getTransactionCount();
        long current = localNonce.get();

        if (current == -1 || current < chainNonce.longValue()) {
            localNonce.set(chainNonce.longValue());
            log.warn("同步链上nonce, address={}, localNonce={}, chainNonce={}", fromAddress, current, chainNonce);
        }

        long useNonce = localNonce.getAndIncrement();
        log.info("分配nonce, address={}, chainNonce={}, useNonce={}, nextLocalNonce={}",
                fromAddress, chainNonce, useNonce, localNonce.get());

        return BigInteger.valueOf(useNonce);
    }

    private void resetLocalNonce(String reason) {
        localNonce.set(-1);
        log.warn("本地nonce已重置, address={}, reason={}", fromAddress, reason);
    }

    private boolean needResetNonce(String msg) {
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("nonce too low")
                || msg.contains("nonce is too low")
                || msg.contains("replacement transaction underpriced")
                || msg.contains("already known")
                || msg.contains("known transaction")
                || msg.contains("transaction underpriced")
                || msg.contains("future transaction tries to replace pending")
                || msg.contains("insufficient funds");
    }

    private BigInteger getGasPrice() throws Exception {
      BigInteger MIN_GAS_PRICE = BigInteger.valueOf(1_000_000_000L); // 1 Gwei
        EthGasPrice gasPriceResp = web3j.ethGasPrice().send();
        if (gasPriceResp == null || gasPriceResp.getGasPrice() == null) {
            return MIN_GAS_PRICE;
        }
        BigInteger gasPrice = gasPriceResp.getGasPrice();
        if (gasPrice.compareTo(MIN_GAS_PRICE) < 0) {
            gasPrice = MIN_GAS_PRICE;
        }
        return gasPrice;
    }

    /**
     * 预估 gas
     */
    private BigInteger estimateGas(String contractAddress, String encodedFunction, BigInteger value) {
        try {
            Transaction tx = Transaction.createFunctionCallTransaction(
                    fromAddress,
                    null,
                    null,
                    null,
                    contractAddress,
                    value == null ? BigInteger.ZERO : value,
                    encodedFunction
            );

            EthEstimateGas estimateGas = web3j.ethEstimateGas(tx).send();

            if (estimateGas != null && !estimateGas.hasError() && estimateGas.getAmountUsed() != null) {
                return estimateGas.getAmountUsed().multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
            }

            if (estimateGas != null && estimateGas.hasError()) {
                log.warn("estimateGas失败, error={}", estimateGas.getError().getMessage());
            }
        } catch (Exception e) {
            log.warn("estimateGas异常, 使用默认gasLimit", e);
        }

        return DefaultGasProvider.GAS_LIMIT;
    }

    /**
     * 从 KMS 公钥推导 EVM 地址
     */
    private String getSignerAddressFromKms() {
        try {
            GetPublicKeyRequest request = GetPublicKeyRequest.builder()
                    .keyId(kmsKeyId)
                    .build();

            GetPublicKeyResponse response = kmsClient.getPublicKey(request);
            byte[] publicKeyDer = response.publicKey().asByteArray();

            String publicKeyHex = extractUncompressedPublicKeyHex(publicKeyDer);
            return "0x" + publicKeyHexToAddress(publicKeyHex);
        } catch (Exception e) {
            throw new RuntimeException("从KMS获取交易发送地址失败", e);
        }
    }

    /**
     * 从 DER 公钥中提取 X + Y
     */
    private String extractUncompressedPublicKeyHex(byte[] derPublicKey) {
        if (derPublicKey == null || derPublicKey.length < 65) {
            throw new IllegalArgumentException("KMS公钥长度异常");
        }

        for (int i = derPublicKey.length - 65; i >= 0; i--) {
            if ((derPublicKey[i] & 0xFF) == 0x04) {
                byte[] key65 = Arrays.copyOfRange(derPublicKey, i, i + 65);
                byte[] key64 = Arrays.copyOfRange(key65, 1, 65);
                return Numeric.toHexStringNoPrefix(key64);
            }
        }

        throw new IllegalArgumentException("无法从KMS DER公钥提取未压缩公钥");
    }

    /**
     * DER 签名解析 r/s
     */
    private BigInteger[] parseDerSignature(byte[] derSignature) {
        int index = 0;

        if (derSignature[index++] != 0x30) {
            throw new RuntimeException("DER签名格式错误: 期望0x30");
        }

        int length = derSignature[index++] & 0xFF;
        if (length != derSignature.length - 2) {
            throw new RuntimeException("DER签名格式错误: 长度不匹配");
        }

        if (derSignature[index++] != 0x02) {
            throw new RuntimeException("DER签名格式错误: 期望r标记0x02");
        }

        int rLen = derSignature[index++] & 0xFF;
        byte[] rBytes = Arrays.copyOfRange(derSignature, index, index + rLen);
        index += rLen;

        if (derSignature[index++] != 0x02) {
            throw new RuntimeException("DER签名格式错误: 期望s标记0x02");
        }

        int sLen = derSignature[index++] & 0xFF;
        byte[] sBytes = Arrays.copyOfRange(derSignature, index, index + sLen);

        return new BigInteger[]{
                new BigInteger(1, rBytes),
                new BigInteger(1, sBytes)
        };
    }

    private String publicKeyHexToAddress(String publicKeyHex) {
        byte[] pubBytes = Numeric.hexStringToByteArray(publicKeyHex);
        byte[] hash = Hash.sha3(pubBytes);

        byte[] addrBytes = new byte[20];
        System.arraycopy(hash, 12, addrBytes, 0, 20);

        return Numeric.toHexStringNoPrefix(addrBytes);
    }

    private String publicKeyToAddress(BigInteger publicKey) {
        byte[] pubBytes = Numeric.toBytesPadded(publicKey, 64);
        byte[] hash = Hash.sha3(pubBytes);

        byte[] addrBytes = new byte[20];
        System.arraycopy(hash, 12, addrBytes, 0, 20);

        return Numeric.toHexStringNoPrefix(addrBytes);
    }
}