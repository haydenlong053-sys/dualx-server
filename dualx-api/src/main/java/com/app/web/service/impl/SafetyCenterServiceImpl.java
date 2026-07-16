package com.app.web.service.impl;

import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.enums.SmsStatusEnum;
import com.app.common.exception.DcException;
import com.app.common.util.*;
import com.app.db.entity.User;
import com.app.db.entity.UserSafety;
import com.app.web.api.req.GoogleBindingReq;
import com.app.web.api.req.MailReq;
import com.app.web.api.req.NewMailReq;
import com.app.web.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SafetyCenterServiceImpl implements SafetyCenterService {

    @Resource
    private IUserService userService;

    @Resource
    private GoogleAuthenticatorUtil googleAuthenticatorUtil;

    @Resource
    private SmsService smsService;

    @Resource
    private IUserSafetyService userSafetyService;
    /**
     * 校验谷歌验证码是否正确
     * @return
     */
    @Override
    public boolean verifyGoogle(String verified) {
        if(StringUtils.isBlank(verified)){
            throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE);
        }
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        LambdaQueryWrapper<UserSafety> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSafety::getUserId,memberId);
        UserSafety userSafety = userSafetyService.getOne(wrapper);

        if(StringUtils.isBlank(userSafety.getGoogle())){
            userSafety.setGoogle(RedisUtil.get("GOOGLE"+memberId));
        }
        String key = RedisKeyUtil.googleVerificationCodeTimes(memberId);
        Integer number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.REQUESTS_ARE_RESTRICTED);
        }
        if(StringUtils.isBlank(userSafety.getGoogle())){
            throw new DcException(BaseResultCodeEnum.BIND_GOOGLE);
        }
        Boolean pass = googleAuthenticatorUtil.verifyCode(userSafety.getGoogle(),Integer.parseInt(verified));
        if(!pass){
            RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
        }else{
            RedisUtil.del(key);
        }
        return pass;
    }

    /**
     * 校验邮箱验证码是否正确
     * @param smsStatusEnum   验证码类型
     * @param mail     邮箱
     * @param value    验证码
     * @return
     */
    @Override
    public boolean verifyMail(SmsStatusEnum smsStatusEnum,String mail,String value) {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        if(mail == null){
            LambdaQueryWrapper<UserSafety> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserSafety::getUserId,memberId);
            UserSafety userSafety = userSafetyService.getOne(wrapper);
            boolean rest = emailFormat(userSafety.getMail());
            if(!rest){
                throw new DcException(BaseResultCodeEnum.PLEASE_BIND_YOUR_EMAIL_FIRST);
            }
            mail = userSafety.getMail();
        }
        if(smsStatusEnum == null){
            throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
        }
        String key = RedisKeyUtil.limitMailNumber(memberId);
        Integer number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.REQUESTS_ARE_RESTRICTED);
        }
        String keyValue = smsStatusEnum.getRemark()+mail+memberId;
        if(!value.equals(RedisUtil.get(keyValue))){
            RedisUtil.setEx(key,(number+1)+"",60 * 10);
            throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
        }
        RedisUtil.del(key);
        return true;
    }

    /**
     * 校验注册邮箱验证码是否正确
     * @param smsStatusEnum   验证码类型
     * @param mail     邮箱
     * @param value    验证码
     * @return
     */
    @Override
    public boolean verifyMailCode(SmsStatusEnum smsStatusEnum,String mail,String value) {
        if(smsStatusEnum == null){
            throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
        }
        String key = smsStatusEnum.getRemark() + mail + "_number";
        Integer number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.REQUESTS_ARE_RESTRICTED);
        }
        String keyValue = smsStatusEnum.getRemark() + mail;
        if(!value.equals(RedisUtil.get(keyValue))){
            RedisUtil.setEx(key,(number+1)+"",60 * 10);
            throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
        }
        RedisUtil.del(key);
        return true;
    }
    /**
     * 绑定谷歌验证
     * @param googleBindingReq
     */
    @Override
    public void bindGoogle(GoogleBindingReq googleBindingReq) {
        String verified = googleBindingReq.getVerified();
        if(StringUtils.isBlank(verified)){
            throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE);
        }
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        UserSafety userSafety = this.addUserSafety(memberId);
        if(!StringUtils.isBlank(userSafety.getGoogle())){
            throw new DcException(BaseResultCodeEnum.GOOGLE_BOUND);
        }
        userSafety.setGoogle(RedisUtil.get("GOOGLE"+memberId));
        if(StringUtils.isBlank(userSafety.getGoogle())){
            throw new DcException(BaseResultCodeEnum.FORBIDDEN);
        }
        String key = RedisKeyUtil.googleVerificationCodeTimes(memberId);
        Integer number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.VERIFICATION_FAILED_OFF);
        }
        if(StringUtils.isBlank(userSafety.getGoogle())){
            throw new DcException(BaseResultCodeEnum.BIND_GOOGLE);
        }
        boolean pass = googleAuthenticatorUtil.verifyCode(userSafety.getGoogle(),Integer.parseInt(verified));
        if(!pass){
            number ++;
            RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
            throw new DcException(BaseResultCodeEnum.GOOGLE_VERIFICATION_FAILED);
        }else{
            RedisUtil.del(key);
        }
        userSafetyService.saveOrUpdate(userSafety);
    }

    /**
     * 发送邮箱验证码
     * @param mailReq
     */
    @Override
    public void sendEmail(MailReq mailReq) {
        String lang = RequestUtil.getAcceptLanguage();
        String mail = mailReq.getMail();
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        UserSafety userSafety = this.addUserSafety(memberId);
        boolean rest = emailFormat(userSafety.getMail());
        String meml = mailReq.getMail().toLowerCase();
        if(mailReq.getStatus().equals(1)){
            if(StringUtils.isBlank(mail) || rest){
                throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
            }
        }else{
            if(!rest){
                throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
            }
            if(StringUtils.isBlank(mailReq.getMail())){
                meml = userSafety.getMail().toLowerCase();
            }
        }
        if(mailReq.getStatus().equals(1) || mailReq.getStatus().equals(3)){
            String mailKey = "MAIL_NUMBER"+memberId;
            if(RedisUtil.get(mailKey) != null){
                throw new DcException(BaseResultCodeEnum.DAY_MAIL_LIMIT);
            }
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(User::getAccount,meml);
            long count = userService.count(userLambdaQueryWrapper);
            if(mailReq.getStatus().equals(1)){
                if(count > 0){
                    throw new DcException(BaseResultCodeEnum.THE_EMAIL_ADDRESS_ALREADY_EXISTS);
                }
            }
        }
        SmsStatusEnum smsStatusEnum = SmsStatusEnum.valueOf(mailReq.getStatus(), lang);
        if(smsStatusEnum == null || mailReq.getStatus() % 2 == 0){
            throw new DcException(BaseResultCodeEnum.THE_VERIFICATION_CODE_FAILED_TO_BE_SENT);
        }
        String key = smsStatusEnum.getRemark()+memberId;
        int number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.FREQUENTLY_SENT);
        }
        RedisUtil.setEx(key,(number+1)+"",60 * 30);

        String code = NumberUtil.generateSmsCode();
        if(!smsService.sendSmsEmailMordhuyun(meml,SmsStatusEnum.valueOf(mailReq.getStatus(), lang),code)){
            throw new DcException(BaseResultCodeEnum.SEND_VERIFICATION_CODE_ERROR);
        }
        key = smsStatusEnum.getRemark()+meml+memberId;
        log.info("邮箱:{},status:{},邮箱验证码:{}",meml,mailReq,code);
        RedisUtil.setEx(key,code,60 * 10);
    }

    @Override
    public void sendEmailCode(MailReq mailReq) {
        String lang = RequestUtil.getAcceptLanguage();
        // 查询当前邮箱是否已经注册 如果注册直接提示去登陆
        User user = userService.findUserByAccount(mailReq.getMail());
        if(user != null){
            throw new DcException(BaseResultCodeEnum.THE_EMAIL_ADDRESS_ALREADY_EXISTS);
        }
        SmsStatusEnum smsStatusEnum = SmsStatusEnum.valueOf(mailReq.getStatus(), lang);
        if(smsStatusEnum == null || mailReq.getStatus() % 2 == 0){
            throw new DcException(BaseResultCodeEnum.THE_VERIFICATION_CODE_FAILED_TO_BE_SENT);
        }
        if(StringUtils.isBlank(mailReq.getMail()) || !emailFormat(mailReq.getMail())){
            throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
        }
        String key = smsStatusEnum.getRemark() + mailReq.getMail() + "_number";
        int number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.FREQUENTLY_SENT);
        }
        RedisUtil.setEx(key,(number+1)+"",60 * 30);

        String code = NumberUtil.generateSmsCode();
        try {
            Boolean sent = smsService.sendSmsEmailMordhuyun(mailReq.getMail(), SmsStatusEnum.valueOf(mailReq.getStatus(), lang), code);
            if (!sent) {
                throw new DcException(BaseResultCodeEnum.SEND_VERIFICATION_CODE_ERROR);
            }
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.SEND_VERIFICATION_CODE_ERROR);
        }
        key = smsStatusEnum.getRemark() + mailReq.getMail();
        log.info("邮箱:{},status:{},邮箱验证码:{}",mailReq.getMail(),mailReq.getStatus(),code);
        RedisUtil.setEx(key,code,60 * 10);
    }

    /**
     * 验证邮箱格式是否正确
     * @param emil
     * @return
     */
    @Override
    public  boolean emailFormat(String emil){
        if(emil == null){
            return false;
        }
        String emailRegx = "\\w{2,}@(\\w{2,20}\\.\\w{2,10}){1,2}";
        boolean ret = org.springframework.util.StringUtils.hasText(emil) && emil.contains("@") &&
            emil.contains(".") && emil.length() > 5 && emil.length() < 30;
        return ret && emil.matches(emailRegx);
    }

    /**
     * 绑定邮箱
     * @param mailReq
     */
    @Override
    public void bindTheEmail(NewMailReq mailReq) {
        String lang = RequestUtil.getAcceptLanguage();
        if(mailReq.getStatus().equals(1) && (StringUtils.isBlank(mailReq.getNewMail()) || StringUtils.isBlank(mailReq.getNewVerified()))){
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        if((mailReq.getStatus().equals(3) && StringUtils.isBlank(mailReq.getVerified()))||
            (StringUtils.isBlank(mailReq.getNewMail()) || StringUtils.isBlank(mailReq.getNewVerified()))){
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        if(!mailReq.getStatus().equals(1) && !mailReq.getStatus().equals(3)){
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        if (mailReq.getStatus().equals(1) && !StringUtils.isBlank(mailReq.getNewMail())) {
            String mail = mailReq.getNewMail().toLowerCase();
            LambdaQueryWrapper<UserSafety> safetyWrapper = new LambdaQueryWrapper<>();
            safetyWrapper.eq(UserSafety::getMail, mail);
            List<UserSafety> safety = userSafetyService.list(safetyWrapper);

            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getAccount, mail);
            User one = userService.getOne(userWrapper);
            if (!safety.isEmpty() || one != null) {
                throw new DcException(BaseResultCodeEnum.EMAIL_MESSAGE_ERROR);
            }
        }

        mailReq.setNewMail(mailReq.getNewMail().trim().toLowerCase());
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        boolean rest = emailFormat(mailReq.getNewMail());
        if(!rest){
            throw new DcException(BaseResultCodeEnum.EMAIL_MESSAGE_ERROR);
        }
        SmsStatusEnum smsStatusEnum = SmsStatusEnum.valueOf(mailReq.getStatus(), lang);
        if(smsStatusEnum == null){
            throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
        }
        String key = smsStatusEnum.getRemark()+"LIMIT" + memberId;
        int number = RedisUtil.getNumber(key);
        if(number > 6){
            throw new DcException(BaseResultCodeEnum.VERIFICATION_FAILED_OFF);
        }

        UserSafety userSafety = this.addUserSafety(memberId);

        String codeKey = smsStatusEnum.getRemark()+mailReq.getNewMail().toLowerCase()+memberId;
        if(!mailReq.getNewVerified().equals(RedisUtil.get(codeKey))){
            RedisUtil.setEx(key,(number+1)+"",60 * 20);
            throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
        }
        if(mailReq.getStatus().equals(3)){
            codeKey = smsStatusEnum.getRemark()+userSafety.getMail().toLowerCase()+memberId;
            if(!mailReq.getVerified().equals(RedisUtil.get(codeKey))){
                RedisUtil.setEx(key,(number+1)+"",60 * 20);
                throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
            }
        }

        //1天只能修改1次
        /*String mailKey = "MAIL_NUMBER"+memberId;
        RedisUtil.setEx(mailKey,mailKey,60 * 60 * 24);*/

        RedisUtil.del(key);
        userSafety.setMail(mailReq.getNewMail().toLowerCase());
        userSafetyService.updateById(userSafety);
    }

    /**
     * 添加用户安全中心配置
     * @param userId     用户ID
     */
    @Override
    public UserSafety addUserSafety(Integer userId){
        UserSafety userSafety = userSafetyService.getById(userId);
        if(userSafety != null){
            return userSafety;
        }
        userSafety = new UserSafety();
        userSafety.setId(userId);
        userSafety.setUserId(userId);
        userSafety.setStatus(1);
        userSafety.setCreateTime(LocalDateTime.now());
        userSafetyService.saveOrUpdate(userSafety);
        return userSafety;
    }

    @Override
    public void sendPasswordEmail(MailReq mailReq) {
        String lang = RequestUtil.getAcceptLanguage();
        SmsStatusEnum smsStatusEnum = SmsStatusEnum.valueOf(mailReq.getStatus(), lang);
        if(smsStatusEnum == null || mailReq.getStatus() % 2 == 0){
            throw new DcException(BaseResultCodeEnum.THE_VERIFICATION_CODE_FAILED_TO_BE_SENT);
        }
        if(StringUtils.isBlank(mailReq.getMail()) || !emailFormat(mailReq.getMail())){
            throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
        }
        String key = smsStatusEnum.getRemark() + mailReq.getMail() + "_number";
        int number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.FREQUENTLY_SENT);
        }
        RedisUtil.setEx(key,(number+1)+"",60 * 30);

        String code = NumberUtil.generateSmsCode();
        try {
            Boolean sent = smsService.sendSmsEmailMordhuyun(mailReq.getMail(), SmsStatusEnum.valueOf(mailReq.getStatus(), lang), code);
            if (!sent) {
                throw new DcException(BaseResultCodeEnum.SEND_VERIFICATION_CODE_ERROR);
            }
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.SEND_VERIFICATION_CODE_ERROR);
        }
        key = smsStatusEnum.getRemark() + mailReq.getMail();
        RedisUtil.setEx(key,code,60 * 10);
    }
}
