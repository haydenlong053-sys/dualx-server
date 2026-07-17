package com.app.web.api.req;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "WithdrawalAuditReq对象", description = "提现审核请求")
public class WithdrawalAuditReq {

    @Schema(description ="申请记录ID")
    private Integer withdrawalLogId;

    @Schema(description ="是否通过 1:通过 0:驳回")
    private Integer approved;

    @Schema(description ="备注")
    private String remark;

    @Schema(description ="签名人地址")
    private String signerAddress;

    @Schema(description ="签名摘要")
    private String signDigest;

    @Schema(description ="签名结果")
    private String signature;

    @Schema(description ="审核失败原因")
    private String failReason;
}