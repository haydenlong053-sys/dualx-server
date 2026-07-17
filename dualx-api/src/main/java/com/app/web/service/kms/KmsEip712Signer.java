package com.app.web.service.kms;

import com.app.common.dto.WithdrawRequest;

import com.app.web.api.resp.SignResult;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KMS EIP-712 签名服务类
 *
 * 作用：
 * 1. 使用 AWS KMS 对 EIP-712 digest 进行签名
 * 2. 将 KMS 返回的 DER 签名转换成以太坊 r + s + v 格式
 * 3. 从 KMS 公钥推导 signer 钱包地址
 * 4. 缓存 signerAddress，避免每次签名都请求 KMS 公钥
 */
public class KmsEip712Signer {

    private static final BigInteger SECP256K1_N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    private static final BigInteger SECP256K1_HALF_N =
            SECP256K1_N.divide(BigInteger.valueOf(2));
    /**
     * signerAddress 缓存
     *
     * key   = kmsKeyId
     * value = 由 KMS 公钥推导出来的钱包地址
     *
     * 说明：
     * 1. 同一个 KMS Key 的 signerAddress 是固定的
     * 2. 没必要每次签名都调用 getPublicKey
     * 3. 使用 ConcurrentHashMap 保证多线程安全
     */
    private static final ConcurrentHashMap<String, String> SIGNER_ADDRESS_CACHE = new ConcurrentHashMap<String, String>();

    private final KmsClient kmsClient;
    private final String kmsKeyId;

    /**
     * 当前 KMS Key 对应的钱包地址
     *
     * 注意：
     * 这个字段仍然保留，方便当前对象内快速使用。
     * 真正避免重复请求 KMS 的是 SIGNER_ADDRESS_CACHE。
     */
    private final String cachedSignerAddress;

    private final BigInteger chainId;
    private final String verifyingContract;

    public KmsEip712Signer(String region, String kmsKeyId, BigInteger chainId, String verifyingContract) {
        this.kmsClient = KmsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.kmsKeyId = kmsKeyId;
        this.chainId = chainId;
        this.verifyingContract = verifyingContract;

        /*
         * 核心优化点：
         *
         * 原来：
         * 每次 new KmsEip712Signer 都会执行 getSignerAddressFromKms()
         * 也就是每次签名都请求一次 KMS getPublicKey。
         *
         * 现在：
         * 同一个 kmsKeyId 只会第一次请求 KMS。
         * 后续直接从 SIGNER_ADDRESS_CACHE 里取。
         */
        this.cachedSignerAddress = SIGNER_ADDRESS_CACHE.computeIfAbsent(
                kmsKeyId,
                key -> getSignerAddressFromKms()
        );
    }

    /**
     * 对提现请求进行 EIP-712 签名
     */
    public SignResult signWithdrawRequest(WithdrawRequest req) throws Exception {
        // 1. 构建 EIP-712 domainSeparator
        byte[] domainSeparator = Eip712Helper.buildDomainSeparator(chainId, verifyingContract);

        // 2. 构建 WithdrawRequest structHash
        byte[] structHash = Eip712Helper.buildWithdrawStructHash(req);

        // 3. 构建最终 EIP-712 digest
        byte[] eip712Digest = Eip712Helper.buildEip712Digest(domainSeparator, structHash);
        String digestHex = Numeric.toHexString(eip712Digest);

        // 4. 使用 KMS 对 digest 签名
        String signatureHex = signDigestWithKms(eip712Digest);

        // 5. 返回 signer 地址、digest、签名
        return new SignResult(cachedSignerAddress, digestHex, signatureHex);
    }

    /**
     * 对已有 digest 进行签名
     */
    public String signDigest(byte[] digest) throws Exception {
        return signDigestWithKms(digest);
    }

    /**
     * 获取当前 KMS Key 对应的钱包地址
     */
    public String getSignerAddress() {
        return cachedSignerAddress;
    }

    /**
     * 清空 signerAddress 缓存
     *
     * 适用场景：
     * 1. 你更换了 KMS Key
     * 2. 测试环境想强制重新读取公钥
     *
     * 正常生产环境一般不需要调用。
     */
    public static void clearSignerAddressCache() {
        SIGNER_ADDRESS_CACHE.clear();
    }

    /**
     * 清空指定 KMS Key 的 signerAddress 缓存
     */
    public static void clearSignerAddressCache(String kmsKeyId) {
        if (kmsKeyId != null) {
            SIGNER_ADDRESS_CACHE.remove(kmsKeyId);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 使用 KMS 签名，并返回以太坊格式签名
     */
    private String signDigestWithKms(byte[] digest) throws Exception {
        SignRequest signRequest = SignRequest.builder()
                .keyId(kmsKeyId)
                .message(SdkBytes.fromByteBuffer(ByteBuffer.wrap(digest)))
                .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
                .messageType(MessageType.DIGEST)
                .build();

        SignResponse signResponse = kmsClient.sign(signRequest);
        byte[] derSignature = signResponse.signature().asByteArray();

        return convertDerToEthSignature(derSignature, digest);
    }

    /**
     * 将 KMS 返回的 DER 签名转换为以太坊签名格式
     *
     * KMS 返回：
     * DER(r, s)
     *
     * 以太坊需要：
     * r 32字节 + s 32字节 + v 1字节
     */
    private String convertDerToEthSignature(byte[] derSignature, byte[] digest) {
        BigInteger[] rs = parseDerSignature(derSignature);

        BigInteger r = rs[0];
        BigInteger s = rs[1];

        // 关键：KMS 返回的 s 可能是 high-s，OpenZeppelin ECDSA 不接受
        if (s.compareTo(SECP256K1_HALF_N) > 0) {
            s = SECP256K1_N.subtract(s);
        }

        // 必须在 low-s 之后再恢复 v
        int recid = recoverRecid(digest, r, s);
        int v = recid + 27;

        byte[] rBytes = to32Bytes(r);
        byte[] sBytes = to32Bytes(s);

        byte[] ethSignature = new byte[65];
        System.arraycopy(rBytes, 0, ethSignature, 0, 32);
        System.arraycopy(sBytes, 0, ethSignature, 32, 32);
        ethSignature[64] = (byte) v;

        return Numeric.toHexString(ethSignature);
    }

    /**
     * 解析 DER 编码的签名
     */
    private BigInteger[] parseDerSignature(byte[] derSignature) {
        int index = 0;

        if (derSignature[index++] != 0x30) {
            throw new RuntimeException("DER 格式错误: 期望 0x30");
        }

        int length = derSignature[index++] & 0xFF;
        if (length != derSignature.length - 2) {
            throw new RuntimeException("DER 格式错误: 长度不匹配");
        }

        if (derSignature[index++] != 0x02) {
            throw new RuntimeException("DER 格式错误: 期望 r 的 0x02");
        }

        int rLen = derSignature[index++] & 0xFF;
        byte[] rBytes = Arrays.copyOfRange(derSignature, index, index + rLen);
        index += rLen;

        if (derSignature[index++] != 0x02) {
            throw new RuntimeException("DER 格式错误: 期望 s 的 0x02");
        }

        int sLen = derSignature[index++] & 0xFF;
        byte[] sBytes = Arrays.copyOfRange(derSignature, index, index + sLen);

        return new BigInteger[]{
                new BigInteger(1, rBytes),
                new BigInteger(1, sBytes)
        };
    }

    /**
     * 恢复 recid
     *
     * 说明：
     * KMS 只返回 r、s，不返回 v。
     * 所以这里通过尝试 recid=0~3，恢复出地址。
     * 如果恢复出来的地址等于 cachedSignerAddress，就说明这个 recid 是正确的。
     */
    private int recoverRecid(byte[] digest, BigInteger r, BigInteger s) {
        for (int recid = 0; recid < 4; recid++) {
            try {
                ECDSASignature ecdsaSignature = new ECDSASignature(r, s);
                BigInteger recoveredPublicKey = Sign.recoverFromSignature(recid, ecdsaSignature, digest);

                if (recoveredPublicKey != null) {
                    String recoveredAddress = publicKeyToAddress(recoveredPublicKey);

                    if (recoveredAddress.equalsIgnoreCase(remove0x(cachedSignerAddress))) {
                        return recid;
                    }
                }
            } catch (Exception ignored) {
                // 当前 recid 不对，继续尝试下一个
            }
        }

        throw new RuntimeException("无法恢复 recid，KMS签名无法匹配 signerAddress=" + cachedSignerAddress);
    }

    /**
     * 从 KMS 获取公钥，并推导 signer 钱包地址
     */
    private String getSignerAddressFromKms() {
        try {
            GetPublicKeyRequest request = GetPublicKeyRequest.builder()
                    .keyId(kmsKeyId)
                    .build();

            GetPublicKeyResponse response = kmsClient.getPublicKey(request);
            byte[] publicKeyDer = response.publicKey().asByteArray();

            /*
             * AWS KMS 返回的不是裸公钥。
             *
             * 不是：
             * 04 + X + Y
             *
             * 而是：
             * ASN.1 DER SubjectPublicKeyInfo
             *
             * 所以必须从 DER 里面提取真正的 secp256k1 未压缩公钥。
             */
            String uncompressedPublicKeyHex = extractUncompressedPublicKeyHex(publicKeyDer);

            return "0x" + publicKeyHexToAddress(uncompressedPublicKeyHex);
        } catch (Exception e) {
            throw new RuntimeException("从 KMS 获取签名者地址失败", e);
        }
    }

    /**
     * 从 AWS KMS DER 公钥中提取未压缩公钥
     *
     * 返回：
     * X + Y，共 64 字节，不包含前面的 0x04。
     *
     * 为什么要这样写：
     * AWS KMS getPublicKey 返回的是 DER 格式。
     * DER 内部才包含真正的未压缩公钥：
     *
     * 04 + X(32字节) + Y(32字节)
     *
     * 以太坊计算地址时，只需要 X + Y，不需要 04。
     */
    private String extractUncompressedPublicKeyHex(byte[] derPublicKey) {
        if (derPublicKey == null || derPublicKey.length < 65) {
            throw new IllegalArgumentException("KMS公钥长度异常，无法解析");
        }

        /*
         * 从后往前找 0x04 更稳。
         * 因为 DER 前面也可能出现 0x04，
         * 但真正的未压缩 secp256k1 公钥一定是：
         * 0x04 + 64字节
         */
        for (int i = derPublicKey.length - 65; i >= 0; i--) {
            if ((derPublicKey[i] & 0xFF) == 0x04) {
                byte[] key65 = Arrays.copyOfRange(derPublicKey, i, i + 65);

                // 去掉第一个 0x04，只保留 X + Y
                byte[] key64 = Arrays.copyOfRange(key65, 1, 65);

                return Numeric.toHexStringNoPrefix(key64);
            }
        }

        throw new IllegalArgumentException("无法从KMS DER公钥中提取未压缩公钥");
    }

    /**
     * 从公钥十六进制字符串推导以太坊地址
     *
     * 输入：
     * X + Y，64字节，不包含 0x04。
     *
     * 规则：
     * address = keccak256(publicKey)[12:]
     */
    private String publicKeyHexToAddress(String publicKeyHex) {
        byte[] pubBytes = Numeric.hexStringToByteArray(publicKeyHex);
        byte[] hash = org.web3j.crypto.Hash.sha3(pubBytes);

        byte[] addrBytes = new byte[20];
        System.arraycopy(hash, 12, addrBytes, 0, 20);

        return Numeric.toHexStringNoPrefix(addrBytes);
    }

    /**
     * 从恢复出来的公钥 BigInteger 推导地址
     */
    private String publicKeyToAddress(BigInteger publicKey) {
        byte[] pubBytes = Numeric.toBytesPadded(publicKey, 64);
        byte[] hash = org.web3j.crypto.Hash.sha3(pubBytes);

        byte[] addrBytes = new byte[20];
        System.arraycopy(hash, 12, addrBytes, 0, 20);

        return Numeric.toHexStringNoPrefix(addrBytes);
    }

    /**
     * BigInteger 转 32 字节
     *
     * r、s 必须固定 32 字节。
     */
    private byte[] to32Bytes(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length == 32) {
            return bytes;
        }

        byte[] padded = new byte[32];

        if (bytes.length < 32) {
            System.arraycopy(bytes, 0, padded, 32 - bytes.length, bytes.length);
        } else {
            /*
             * BigInteger 可能因为符号位多出一个 0x00，
             * 这里取最后 32 字节。
             */
            System.arraycopy(bytes, bytes.length - 32, padded, 0, 32);
        }

        return padded;
    }

    /**
     * 去掉地址前缀 0x
     */
    private String remove0x(String value) {
        if (value == null) {
            return null;
        }

        if (value.startsWith("0x") || value.startsWith("0X")) {
            return value.substring(2);
        }

        return value;
    }

    private void close() {

    }
}