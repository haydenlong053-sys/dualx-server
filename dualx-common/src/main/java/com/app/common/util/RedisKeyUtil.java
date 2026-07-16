package com.app.common.util;



/**
 * redisKey
 * @author hayden
 */
public class RedisKeyUtil {

    /**
     * 转账和提币KEY
     * @return
     */
    public static String payTheAccountKey(Integer userId){
        return "WITHDRAWAL_AND_TRANSFER_LIMIT:"+userId;
    }


    /**
     * 转账和提币验证次数限制KEY
     * @return
     */
    public static String payTheAccountNumberKey(Integer userId){
        return "WITHDRAWAL_AND_TRANSFER_NUMBER:"+userId;
    }


    /**
     * 密码错误次数限制KEY
     * @return
     */
    public static String passwordNumber(Integer userId){
        return "USER_PAY_PASSWORD:"+userId;
    }

    /**
     * 校验邮箱验证码是否正确次数限制
     * @return
     */
    public static String limitMailNumber(Integer userId){
        return "PAYPASSWORDMAIL:"+userId;
    }

    /**
     * 绑定谷歌验证码次数限制
     * @return
     */
    public static String googleVerificationCodeTimes(Integer userId){
        return "PAYPASSWORDGoogle:"+userId;
    }


    /**
     * 登录密码错误次数限制
     * @return
     */
    public static String logInPasswordNumber(Integer userId){
        return "LOGIN_PASSWORD_NUMBER:"+userId;
    }


    /**
     * 找回密码同一账号次数限制KEY
     * @return
     */
    public static String findPasswordAccountNumber(Integer userId){
        return "FINDPAYPASSWORD_ACCOUNT_NUMBER:" + userId;
    }

    /**
     * 找回密码同一ip次数限制KEY
     * @return
     */
    public static String findPasswordIpNumber(String ip) {
        return "FINDPAYPASSWORD_IP_NUMBER:" + ip;
    }

    /**
     * 修改账号次数限制KEY
     * @return
     */
    public static String modifyAccountNumber(Integer userId) {
        return "MODIFY_ACCOUNT_NUMBER:" + userId;
    }


    /**
     * 用户点赞KEY
     * @return
     */
    public static String likeKey(Integer activityLogId,Integer userId) {
        return "LIKE_KEY:" + activityLogId+"_"+userId;
    }

    /**
     * 商家支付验证次数限制KEY
     * @return
     */
    public static String merchantPayNumber(Integer userId) {
        return "MERCHANT_PAY_NUMBER:" + userId;
    }

    /**
     * 大屏数据显示
     * @return
     */
    public static String getRWAKey() {
        return "RWA_KEY";
    }

}
