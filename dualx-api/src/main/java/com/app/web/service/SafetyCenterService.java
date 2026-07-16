package com.app.web.service;

import com.app.common.enums.SmsStatusEnum;
import com.app.db.entity.UserSafety;
import com.app.web.api.req.GoogleBindingReq;
import com.app.web.api.req.MailReq;
import com.app.web.api.req.NewMailReq;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 安全中心 服务类
 * </p>
 *
 * @author HayDen
 * @since 2025-05-25
 */
public interface SafetyCenterService{

    /**
     * 验证谷歌验证是否正确
     * @param value
     * @return
     */
    boolean verifyGoogle(String value);

    /**
     * 校验邮箱验证码是否正确
     * @param value
     * @return
     */
    boolean verifyMail(SmsStatusEnum smsStatusEnum, String mail, String value);

    /**
     * 校验注册邮箱验证码是否正确
     * @param value
     * @return
     */
    boolean verifyMailCode(SmsStatusEnum smsStatusEnum, String mail, String value);

    /**
     * 绑定谷歌验证码
     * @param googleBindingReq
     */
    void bindGoogle(GoogleBindingReq googleBindingReq);

    /**
     * 发送邮箱验证码
     */
    void sendEmail(MailReq mailReq);

    /**
     * 发送注册邮箱验证码
     */
    void sendEmailCode(MailReq mailReq);

    /**
     * 绑定邮箱
     * @param mailReq
     */
    void bindTheEmail(NewMailReq mailReq);

    /**
     * 校验邮箱格式
     * @param emil
     */
    boolean emailFormat(String emil);

    UserSafety addUserSafety(Integer userId);

    void sendPasswordEmail(MailReq mailReq);
}
