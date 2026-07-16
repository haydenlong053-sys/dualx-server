package com.app.common.enums;

public enum BaseResultCodeEnum implements BaseEnum {

    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "错误的请求"),
    UN_AUTHORIZATION(401, "登陆过期"),
    FORBIDDEN(402, "禁止操作"),
    SERVER_EXCEPTION(500, "服务异常"),


    //-------------------------------------用户相关------------------------------------
    THE_ACCOUNT_DOES_NOT_EXIST(1000, "帐号不存在，如果绑定邮箱请用邮箱登录！"),
    INVALID_INVITATION_CODE(1001, "无效邀请码"),
    NO_ACCOUNT_DETECTED(1002, "没有检测到帐号"),
    REGISTRATION_IS_TOO_FREQUENT(1003, "注册过于频繁"),
    PLEASE_ENTER_THE_ORIGINAL_PASSWORD(1004, "请输入原密码"),
    PASSWORD_LENGTH(1005, "密码长度为6位"),
    PAYMENT_PASSWORD_ERROR(1006, "原支付密码错误"),
    PASSWORD_SETTINGS_FREQUENTLY(1007, "支付密码设置频繁，已被锁定请稍后再试"),
    RESTRICTIONS_ON_MODIFICATION(1008, "密码输入次数过多，暂时限制修改登录密码"),
    REGISTRATION_FAILED(1009, "注册失败"),
    REGISTRATION_SUSPENSION(1010, "暂停注册"),
    INVITE_CONNECTION(1011, "需要邀请连接才能注册"),
    RESTRICTIONS_ON_MODIFY_ACCOUNT(1012, "验证失败次数过多，暂时限制修改登录账号"),
    RESTRICTIONS_ON_FIND_PASSWORD(1013, "验证失败次数过多，暂时限制找回登录密码"),



    //-------------------------------------转账/提币相关------------------------------------
    MAXIMUM_QUOTA(2000, "当日提币超过最大额度"),
    NO_PERMISSION_TO_WITHDRAW_COINS_YET(2001, "暂无提币权限"),
    CHAIN_WITHDRAWAL_CHANNEL_IS_CLOSED(2002, "该链提币通道关闭"),
    MINIMUM_CURRENCY_WITHDRAWAL_LIMIT(2003, "最低提币限额"),
    THIS_CURRENCY_DOES_NOT_SUPPORT_TRANSFER(2004, "该币种不支持转账"),
    TRANSFER_AMOUNT_INCORRECT(2005, "转账金额错误"),
    THE_TRANSFER_USER_DOES_NOT_EXIST(2006, "转账用户不存在"),
    INSUFFICIENT_WALLET_BALANCE(2007, "钱包余额不足"),
    WALLET_DEDUCTION_FAILED(2008, "钱包扣款失败"),
    CAN_T_TRANSFER_MONEY_TO_YOURSELF(2009, "不能给自己转账"),
    ONLY_TRANSFER_MONEY_TO_SUBORDINATES(2010, "只能给下级转账"),
    COIN_WITHDRAWAL_CHANNEL_CLOSED(2011, "提币通道关闭"),
    ACCOUNT_LOCKOUT(2012, "密码连续错误5次,12小时限制提币"),
    WRONG_WALLET_ADDRESS(2013, "钱包地址错误"),
    INTERNAL_ADDRESS(2014, "转账的收款地址必须是平台内部地址"),


    //-------------------------------------购买相关------------------------------------
    PRODUCTS_HAVE_BEEN_REMOVED_FROM_THE_SHELVES(3001, "商品已下架"),
    PRODUCT_PARAMETERS_INCORRECT(3002, "商品下单参数错误"),
    PURCHASE_AND_REMOVE_PRODUCTS(3003, "不能购买下架商品"),
    POINTS_BALANCE_INSUFFICIENT(3004, "积分余额不足"),
    POINTS_AMOUNT_ERROR(3005, "积分金额错误"),
    CANCELLATION_FAILED(3006, "取消订单失败"),
    PAYMENT_PASSWORD_ERROR_TI(3007, "支付密码错误"),
    MEMBERSHIP_HAS_NOT_EXPIRED(3008, "已是会员"),
    SET_A_PAYMENT_PASSWORD(3009, "请先设置支付密码"),
    DETRIMENT_LIMIT(3010, "购买数量超过上限"),
    ONLY_PAY_POINTS(3011, "只能积分支付"),
    PLEASE_UPGRADE_TO_THE_MEMBERSHIP_FIRST(3012, "请先升级会员"),
    PASSWORD_ERR_LIMIT(3013, "密码错误次数限制，请稍后重试"),
    ONE_UNIT_IS_LIMITED_TO_PURCHASE_EVERY_DAY(3014, "单个算力每日限购一台"),
    MEMBERSHIP_PACKAGE_PURCHASE_FAILED(3015, "会员包购买失败"),
    TOKEN_BALANCE_INSUFFICIENT(3016, "代币余额不足"),

    //-------------------------------------安全中心------------------------------------
    ERROR_PASSWORD(4000, "密码错误"),
    NEW_PASSWORD_CANNOT_BE_EMPTY(4001, "新密码不能为空"),
    PASSWORD_FORMAT_ERROR(4002, "密码必须由数字加字母组成长度大于8位"),
    THE_ORIGINAL_LOGIN_PASSWORD_IS_WRONG(4003, "原登录密码错误"),
    VERIFICATION_CODE(4004, "请输入6位验证码"),
    BIND_GOOGLE(4005, "请先绑定谷歌验证"),
    GOOGLE_BOUND(4006, "您已绑定谷歌器"),
    EMAIL_MESSAGE_ERROR(4007, "邮箱信息错误"),
    FREQUENTLY_SENT(4008, "邮箱发送频繁"),
    VERIFICATION_CODE_ERROR(4009, "验证码错误"),
    SEND_VERIFICATION_CODE_ERROR(4010, "发送验证码错误"),
    THE_VERIFICATION_CODE_FAILED_TO_BE_SENT(4011, "验证码发送失败"),
    PLEASE_BIND_YOUR_EMAIL_FIRST(4012, "请先绑定邮箱"),
    VERIFICATION_FAILED(4013, "验证失败"),
    REQUESTS_ARE_RESTRICTED(4014, "连续输错密码,请求被限制!!"),
    THE_EMAIL_ADDRESS_ALREADY_EXISTS(4015, "邮箱已存在"),
    VERIFICATION_FAILED_OFF(4016, "验证失败！请稍后重试"),
    DAY_MAIL_LIMIT(4017, "邮箱一天只能修改/绑定一次哦"),
    OPERATIONAL_RESTRICTIONS(4018, "操作限制,请联系客服"),
    PASSWORD_FORMAT_ERROR8_TO30(4019, "登录密码必须大于8位和小于30位"),
    VERIFICATION_CODE_CONTINUOUS_ERRORS(4020, "验证码连续错误，限制操作12小时"),
    VERIFICATION_EXPIRED(4021, "验证过期，请刷新后重新验证"),
    RESTRICT_EMAIL_SENDING(4022, "邮箱发送频繁已被限制，请稍后再试"),
    GOOGLE_VERIFICATION_FAILED(4023, "谷歌验证失败"),
    EMAIL_VERIFICATION_FAILED(4024, "邮箱验证失败"),
    ACCOUNT_FORMAT_ERROR(4025, "账号必须由数字加字母组成长度大于7位小于16位"),
    ACCOUNT_ALREADY_EXIST(4026, "账号已存在"),
    NO_BIND_GOOGLE_AND_EMIAL(4027, "没有绑定谷歌和邮箱验证，不支持找回密码"),
    MAIL_BIND_MULTIPLE_ACCOUNT(4028, "邮箱绑定了多个账号，不支持找回密码"),
    MAIL_BIND_MULTIPLE_ACCOUNT_LOGIN(4029, "邮箱绑定了多个账号，不支持通过邮箱登录"),

    //-------------------------------------其他------------------------------------
    FILE_CANNOT_BE_EMPTY(5000, "文件不能为空"),
    ERROR_PARAMETERS(5001, "参数错误"),
    FILE_PARAMETERS_ERROR(5002, "多文件上传，参数为files"),
    IT_S_TOO_FREQUENT(5003, "操作过于频繁啦"),
    THIS_FILE_UPLOAD_IS_NOT_SUPPORTED(5004, "不支持该文件上传"),
    EXCEEDED_LIMIT_QUANTITY(5005, "常用地址超过20个，不支持新增"),
    ALREADY_MERCHANT(5006, "您已经成为商家，不支持再申请"),
    LEVEL_NOT_MEET_MERCHANT_APPLY(5007, "会员等级不满足入驻商家的申请条件"),
    PLEASE_UPGRADE_TO_MERCHANT_FIRST(5008, "请先申请成为商家"),
    NO_ACCESS_TO_COMPUTING_POWER(5009, "非会员不可接受算力包"),
    NO_ACCESS_TO_MEMBERSHIP(5010, "会员不可接受会员包"),
    CAN_NOT_SEND_TO_YOURSELF(5011, "商家不可以给自己送算力"),
    CANNOT_ASSIGN_TO_YOURSELF(5012, "不可以给自己分配客户,并且接受人不可以是客户自己"),
    MEMBER_ALREADY_ASSIGNED(5013, "每个客户只能被分配一次"),
    ASSIGN_SCALE_ERROR(5014, "分配比例错误"),
    RECIPIENT_NOT_EXIST(5015, "分配客户接受人不存在"),
    MEMBER_NOT_REFER(5016, "客户不是分配人的直推"),
    CART_NO_MEMBER_ADD(5017, "购物车中只能添加一个会员包"),
    NO_RIGHT_GET_RESERVOIR(5026, "未达到配置会员等级，无权限查看"),
    ;

    private final int code;
    private final String remark;

    BaseResultCodeEnum(int code, String remark) {
        this.code = code;
        this.remark = remark;
    }

    public int getCode() {
        return code;
    }

    public String getRemark() {
        return remark;
    }

    @Override
    public int getEnumCode() {
        return this.code;
    }

    @Override
    public String i18nKey() {
        return "BaseResultCodeEnum." + this.code;
    }

    public static BaseResultCodeEnum valueOf(int enumCode) {
        for (BaseResultCodeEnum typeEnum : BaseResultCodeEnum.values()) {
            if (typeEnum.code == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }
}
