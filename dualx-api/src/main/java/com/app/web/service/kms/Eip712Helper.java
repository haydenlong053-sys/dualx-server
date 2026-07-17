package com.app.web.service.kms;

import com.app.common.dto.WithdrawRequest;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * EIP-712 工具类
 * 负责 domainSeparator、structHash、digest 的计算
 */
public class Eip712Helper {

    // ==================== EIP-712 常量 ====================

    private static final String DOMAIN_NAME = "ExchangeGradeWithdraw";
    private static final String DOMAIN_VERSION = "1";

    private static final byte[] DOMAIN_TYPE_HASH = keccak(
            "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
                    .getBytes(StandardCharsets.UTF_8)
    );

    private static final byte[] WITHDRAW_TYPE_HASH = keccak(
            "WithdrawRequest(uint256 orderId,address user,uint256 amount,uint8 redemption,uint256 deadline,uint256 bizId)"
                    .getBytes(StandardCharsets.UTF_8)
    );

    // ==================== 公共方法 ====================

    /**
     * 构建 domainSeparator
     */
    public static byte[] buildDomainSeparator(BigInteger chainId, String verifyingContract) {
        byte[] nameHash = keccak(DOMAIN_NAME.getBytes(StandardCharsets.UTF_8));
        byte[] versionHash = keccak(DOMAIN_VERSION.getBytes(StandardCharsets.UTF_8));

        byte[] encoded = concat(
                DOMAIN_TYPE_HASH,
                nameHash,
                versionHash,
                uint256ToBytes32(chainId),
                addressToBytes32(verifyingContract)
        );

        return keccak(encoded);
    }

    /**
     * 构建 WithdrawRequest structHash
     */
    public static byte[] buildWithdrawStructHash(WithdrawRequest req) {
        byte[] encoded = concat(
                WITHDRAW_TYPE_HASH,
                uint256ToBytes32(req.getOrderId()),
                addressToBytes32(req.getUser()),
                uint256ToBytes32(req.getAmount()),
                uint256ToBytes32(req.getRedemption()),
                uint256ToBytes32(req.getDeadline()),
                uint256ToBytes32(req.getBizId())
        );

        return keccak(encoded);
    }

    /**
     * 构建最终 EIP-712 摘要
     * keccak256(0x1901 ++ domainSeparator ++ structHash)
     */
    public static byte[] buildEip712Digest(byte[] domainSeparator, byte[] structHash) {
        byte[] prefix = new byte[]{0x19, 0x01};
        return keccak(concat(prefix, domainSeparator, structHash));
    }

    /**
     * 从签名恢复地址（用于验签）
     */
    public static String recoverAddressFromSignature(byte[] digest, String signatureHex) {
        try {
            Sign.SignatureData signatureData = parseSignature(signatureHex);
            int recId = (signatureData.getV()[0] & 0xFF) - 27;
            if (recId < 0 || recId > 3) {
                throw new IllegalArgumentException("签名v值无效: " + (signatureData.getV()[0] & 0xFF));
            }

            BigInteger publicKey = Sign.recoverFromSignature(
                    recId,
                    new org.web3j.crypto.ECDSASignature(
                            new BigInteger(1, signatureData.getR()),
                            new BigInteger(1, signatureData.getS())
                    ),
                    digest
            );

            if (publicKey == null) {
                throw new IllegalStateException("无法从签名恢复公钥");
            }

            return ("0x" + publicKeyToAddress(publicKey)).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("EIP-712 本地验签失败", e);
        }
    }

    // ==================== 私有工具方法 ====================

    private static byte[] keccak(byte[] input) {
        return Hash.sha3(input);
    }

    private static byte[] uint256ToBytes32(BigInteger value) {
        return Numeric.toBytesPadded(value, 32);
    }

    private static byte[] addressToBytes32(String address) {
        String clean = Numeric.cleanHexPrefix(address);
        if (clean.length() != 40) {
            throw new IllegalArgumentException("地址长度不正确: " + address);
        }
        byte[] addr = Numeric.hexStringToByteArray(clean);
        byte[] out = new byte[32];
        System.arraycopy(addr, 0, out, 12, 20);
        return out;
    }

    private static byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] arr : arrays) {
            len += arr.length;
        }
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, out, pos, arr.length);
            pos += arr.length;
        }
        return out;
    }

    private static Sign.SignatureData parseSignature(String signatureHex) {
        byte[] sig = Numeric.hexStringToByteArray(signatureHex);
        if (sig.length != 65) {
            throw new IllegalArgumentException("签名长度必须是65字节");
        }

        byte[] r = new byte[32];
        byte[] s = new byte[32];
        byte[] v = new byte[1];

        System.arraycopy(sig, 0, r, 0, 32);
        System.arraycopy(sig, 32, s, 0, 32);
        v[0] = sig[64];

        if ((v[0] & 0xFF) < 27) {
            v[0] = (byte) ((v[0] & 0xFF) + 27);
        }

        return new Sign.SignatureData(v, r, s);
    }

    private static String publicKeyToAddress(BigInteger publicKey) {
        byte[] pubBytes = Numeric.toBytesPadded(publicKey, 64);
        byte[] hash = Hash.sha3(pubBytes);
        byte[] addrBytes = new byte[20];
        System.arraycopy(hash, 12, addrBytes, 0, 20);
        return Numeric.toHexStringNoPrefix(addrBytes);
    }
}