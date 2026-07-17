package com.app.web.api.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 签名结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignResult {

    /**
     * 签名地址
     */
    private String signerAddress;

    /**
     * 摘要
     */
    private String digest;

    /**
     * 签名结果
     */
    private String signature;

    /**
     * 签名时间
     */
    private LocalDateTime signedAt;

    public SignResult(String signerAddress, String digestHex, String signatureHex) {
        this.signerAddress = signerAddress;
        this.digest = digestHex;
        this.signature = signatureHex;
    }

}