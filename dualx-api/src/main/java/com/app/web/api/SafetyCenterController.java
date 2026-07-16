package com.app.web.api;

import cn.dev33.satoken.stp.StpUtil;
import com.app.common.annotation.Login;
import com.app.common.annotation.SecureRequest;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.enums.SmsStatusEnum;
import com.app.common.exception.DcException;
import com.app.common.util.*;
import com.app.db.entity.User;
import com.app.db.entity.UserSafety;
import com.app.db.entity.Vo.UserPasswordVo;
import com.app.web.api.req.*;
import com.app.web.api.resp.PasswordFindResp;
import com.app.web.api.resp.PasswordTypeResp;
import com.app.web.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping(value = "/api/safetyCenter")
@Tag(name = "安全中心")
@CrossOrigin(origins = "*")
public class SafetyCenterController {

    @Resource
    private IUserService userService;

    @Resource
    private GoogleAuthenticatorUtil googleAuthenticatorUtil;

    @Resource
    private SafetyCenterService safetyCenterService;

    @Resource
    private IUserSafetyService userSafetyService;

    @Login
    @GetMapping(value = "/findPasswordSettingsType")
    @Operation(summary = "安全中心查询用户密码设置情况")
    public PasswordTypeResp findPasswordSettingsType() {
        PasswordTypeResp resp = new PasswordTypeResp();
        resp.setLoginPassword(true);
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        UserSafety userSafety = safetyCenterService.addUserSafety(memberId);
        User user = userService.getById(memberId);
        resp.setPayPassword(!StringUtils.isBlank(userSafety.getPasswordPay()));
        resp.setGoogle(!StringUtils.isBlank(userSafety.getGoogle()));
        if(userSafety.getMail() != null){
            resp.setMail(true);
            resp.setMailAccount(this.maskEmailWithRegex(userSafety.getMail()));
        }else{
            resp.setMail(false);
        }
        resp.setAccount(maskString(user.getAccount()));
        return resp;
    }

    public String maskEmailWithRegex(String email) {
        if (email == null || !email.contains("@")){
            return email;
        }
        String[] parts = email.split("@");
        String prefix = parts[0];
        String domain = parts[1];
        if (prefix.length() <= 4) {
            return prefix.replaceAll(".", "*") + "@" + domain;
        }
        int start = (prefix.length() - 4) / 2;
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = start; i < start + 4 && i < prefix.length(); i++) {
            sb.setCharAt(i, '*');
        }
        return sb + "@" + domain;
    }


    @Login
    @GetMapping(value = "/verifyPaymentPassword")
    @Operation(summary = "验证支付密码是否正确")
    public Map<String, Boolean> verifyPaymentPassword(String password) {
        if(StringUtils.isBlank(password)){
            throw new DcException(BaseResultCodeEnum.PAYMENT_PASSWORD_ERROR);
        }
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        String key = "PAYPASSWORD:"+memberId;
        Integer number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.REQUESTS_ARE_RESTRICTED);
        }
//        User user = userService.getById(memberId);
        UserSafety safety = userSafetyService.getById(memberId);
        Boolean pass = Md5Util.md5(password).equals(safety.getPasswordPay());
        if(!pass){
            RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
        }else{
            RedisUtil.del(key);
        }
        return Collections.singletonMap("result",pass);
    }

    @Login
    @GetMapping(value = "/verifyPasswordByGoogle")
    @Operation(summary = "修改登录密码校验第一个页面")
    public Map<String, Boolean> verifyPasswordByGoogle(PasswordPayReq req) {
        String verification = req.getVerification();
        Integer status = req.getStatus();
        Integer type = req.getType();
        if(StringUtils.isBlank(verification) || status == null || (status != 1 && status != 2) || type == null || (type != 1 && type != 2)){
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());

        String key = type == 1 ? "PAYPASSWORDGOOGLE:"+memberId : "VERIFYPAYPASSWORD:"+memberId;
        Integer number = RedisUtil.getNumber(key);
        if(number > 7){
            throw new DcException(BaseResultCodeEnum.RESTRICTIONS_ON_MODIFICATION);
        }

        LambdaQueryWrapper<UserSafety> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSafety::getUserId,memberId);
        UserSafety userSafety = userSafetyService.getOne(wrapper);

        if(status == 1){
            if(!safetyCenterService.verifyMail(SmsStatusEnum.MODIFY_THE_LOGIN_PASSWORD,userSafety.getMail(),verification)){
                number++;
                RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
                throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
            }
        }else{
            if(!googleAuthenticatorUtil.verifyCode(userSafety.getGoogle(),Integer.parseInt(verification))){
                number++;
                RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
                throw new DcException(BaseResultCodeEnum.GOOGLE_VERIFICATION_FAILED);
            }
        }
        RedisUtil.del(key);
        key = type == 1 ? "VERIFYPASSWORDBYGOOGLE_idx"+memberId : "VERIFYPAYPASSWORD_idx" + memberId;
        RedisUtil.setEx(key,key,5 * 60);
        return Collections.singletonMap("result",true);
    }

    @Login
    @PostMapping(value = "/renewPasswordLogIn")
    @SecureRequest
    @Operation(summary = "修改登录密码")
    public UserPasswordVo renewPasswordLogIn(@RequestBody @Validated PasswordPayReq req) {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        String key = "VERIFYPASSWORDBYGOOGLE_idx"+memberId;
        if(RedisUtil.get(key) == null){
            throw new DcException(BaseResultCodeEnum.VERIFICATION_FAILED);
        }
        User user = userService.getById(memberId);
        /*if(StringUtils.isBlank(req.getVerification()) || req.getStatus() == null ||
            (req.getStatus() != 1 && req.getStatus() != 2)){
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        if(req.getFromPassword() == null){
            throw new DcException(BaseResultCodeEnum.PLEASE_ENTER_THE_ORIGINAL_PASSWORD);
        }*/
        if(StringUtils.isBlank(req.getToPassword())){
            throw new DcException(BaseResultCodeEnum.NEW_PASSWORD_CANNOT_BE_EMPTY);
        }
        if(req.getToPassword().length() > 30 || req.getToPassword().length() < 8){
            throw new DcException(BaseResultCodeEnum.PASSWORD_FORMAT_ERROR8_TO30);
        }
        /*if(!user.getPasswordLogin().equals(req.getFromPassword())){
            throw new DcException(BaseResultCodeEnum.THE_ORIGINAL_LOGIN_PASSWORD_IS_WRONG);
        }
        if(!isValidPassword(req.getToPassword())){
            throw new DcException(BaseResultCodeEnum.PASSWORD_FORMAT_ERROR);
        }
        if(req.getStatus().equals(2)){
            if(StringUtils.isBlank(user.getGoogle())){
                throw new DcException(BaseResultCodeEnum.BIND_GOOGLE);
            }
            if(!this.verifyGoogle(req.getVerification()).get("result")){
                throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
            }
        }else{
            boolean rest = org.springframework.util.StringUtils.hasText(user.getAccount()) && user.getAccount().contains("@") &&
                user.getAccount().contains(".") && user.getAccount().length() > 5;
            if(!rest){
                throw new DcException(BaseResultCodeEnum.PLEASE_BIND_YOUR_EMAIL_FIRST);
            }
            if(!safetyCenterService.verifyMail(SmsStatusEnum.MODIFY_THE_LOGIN_PASSWORD,user.getAccount(),req.getVerification())){
                throw new DcException(BaseResultCodeEnum.VERIFICATION_CODE_ERROR);
            }
        }*/
        UserSafety safety = userSafetyService.getById(memberId);
        safety.setPasswordLogin(req.getToPassword());
        userSafetyService.updateById(safety);
//        userService.updateById(new User().setPasswordLogin(req.getToPassword()).setId(memberId));
        StpUtil.logout(memberId);

        UserPasswordVo userPasswordVo = new UserPasswordVo();
        userPasswordVo.setAccount(user.getAccount());
        userPasswordVo.setPassword(req.getToPassword());
        String count = "account=" + user.getAccount()+"&password="+req.getToPassword();
        String ad = QRCodeUtil.generateQRCodeBase64(count,300,300);
        userPasswordVo.setBase64Img(ad);
        RedisUtil.del(key);
        return userPasswordVo;
    }

    public static boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{4,}$";
        return password != null && password.matches(regex);
    }

    @Login
    @GetMapping(value = "/generateGoogleVerificationCode")
    @Operation(summary = "生成谷歌验证码")
    public Map<String, String> generateGoogleVerificationCode() {
        Map<String, String> map = new HashMap<>();
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        UserSafety userSafety = safetyCenterService.addUserSafety(memberId);
        if(!StringUtils.isBlank(userSafety.getGoogle())){
            throw new DcException(BaseResultCodeEnum.GOOGLE_BOUND);
        }
        String key = RedisUtil.get("GOOGLE"+memberId);
        if(key == null){
            String googleKey = googleAuthenticatorUtil.generateSecretKey();
            key = googleKey;
            RedisUtil.setEx("GOOGLE"+memberId,googleKey,60 * 60 * 24);
        }
        map.put("googleKey",key);
        map.put("googleKeyBase64", googleAuthenticatorUtil.generateQRCodeWithoutAccount(key));
        return map;
    }

    @Login
    @GetMapping(value = "/verifyGoogle")
    @Operation(summary = "验证谷歌验证码是否正确")
    public Map<String, Boolean> verifyGoogle(String verified) {
        return Collections.singletonMap("result", safetyCenterService.verifyGoogle(verified));
    }

    @Login
    @PostMapping(value = "/bindGoogle")
    @SecureRequest
    @Operation(summary = "绑定谷歌验证码")
    public Map<String, Boolean> bindGoogle(@RequestBody @Validated GoogleBindingReq googleBindingReq) {
        safetyCenterService.bindGoogle(googleBindingReq);
        return Collections.singletonMap("result", true);
    }

    @Login
    @PostMapping(value = "/sendEmail")
    @SecureRequest
    @Operation(summary = "发送邮箱验证码")
    public Map<String, Boolean> sendEmail(@RequestBody @Validated MailReq mailReq) {
        safetyCenterService.sendEmail(mailReq);
        return Collections.singletonMap("result", true);
    }

    @PostMapping(value = "/sendEmailCode")
    @SecureRequest
    @Operation(summary = "发送注册邮箱验证码")
    public Map<String, Boolean> sendEmailCode(@RequestBody @Validated MailReq mailReq) {
        safetyCenterService.sendEmailCode(mailReq);
        return Collections.singletonMap("result", true);
    }

    @PostMapping(value = "/sendPasswordEmail")
    @SecureRequest
    @Operation(summary = "发送找回登录密码验证码")
    public Map<String, Boolean> sendPasswordEmail(@RequestBody @Validated MailReq mailReq) {
        LambdaQueryWrapper<UserSafety> safetyLambdaQueryWrapper = new LambdaQueryWrapper<>();
        safetyLambdaQueryWrapper.eq(UserSafety::getFlag, 1).eq(UserSafety::getMail, mailReq.getMail().toLowerCase());
        List<UserSafety> userSafety = userSafetyService.list(safetyLambdaQueryWrapper);
        if (userSafety.isEmpty()) {
            throw new DcException(BaseResultCodeEnum.EMAIL_MESSAGE_ERROR);
        }
        safetyCenterService.sendPasswordEmail(mailReq);
        return Collections.singletonMap("result", true);
    }


    @Login
    @PostMapping(value = "/renewPasswordPay")
    @SecureRequest
    @Operation(summary = "设置或者修改支付密码")
    public Map<String, Boolean> renewPasswordPay(@RequestBody @Validated PasswordPayReq req) {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
//        User user = userService.getById(memberId);
        UserSafety safety = userSafetyService.getById(memberId);
        if (!StringUtils.isBlank(safety.getPasswordPay())){
            String key = "VERIFYPAYPASSWORD_idx"+memberId;
            if(RedisUtil.get(key) == null){
                throw new DcException(BaseResultCodeEnum.VERIFICATION_FAILED);
            }
        }
//        if(!StringUtils.isBlank(user.getPasswordPay()) && StringUtils.isBlank(req.getFromPassword())){
//            throw new DcException(BaseResultCodeEnum.PLEASE_ENTER_THE_ORIGINAL_PASSWORD);
//        }
//        String key = "PASSWORDPAYNUMBER"+memberId;
//        Integer number = RedisUtil.getNumber(key);
//        if(number != null && number > 5){
//            throw new DcException(BaseResultCodeEnum.PASSWORD_SETTINGS_FREQUENTLY);
//        }
//        if(!StringUtils.isBlank(req.getFromPassword()) && !Md5Util.md5(req.getFromPassword()).equals(user.getPasswordPay())){
//            RedisUtil.setEx(key,(number + 1) + "",60 * 60);
//            throw new DcException(BaseResultCodeEnum.PAYMENT_PASSWORD_ERROR);
//        }
//        RedisUtil.del(key);
        //user.setPasswordPay(Md5Util.md5(req.getToPassword()));

        //密码修改成功删除5次限制
        String key = RedisKeyUtil.passwordNumber(memberId);
        RedisUtil.del(key);

        //userService.updateById(user);
        safety.setPasswordPay(Md5Util.md5(req.getToPassword()));
        userSafetyService.updateById(safety);
        return Collections.singletonMap("result", true);
    }

    @Login
    @PostMapping(value = "/bindTheEmail")
    @SecureRequest
    @Operation(summary = "绑定/修改邮箱")
    public Map<String, Boolean> bindTheEmail(@RequestBody @Validated NewMailReq mailReq) {
        safetyCenterService.bindTheEmail(mailReq);
        return Collections.singletonMap("result", true);
    }

    @PostMapping(value = "/getPassword")
    @SecureRequest
    @Operation(summary = "找回密码")
    public String getPassword(@RequestBody @Validated PasswordFindReq req, HttpServletRequest request) {
        UserSafety safety = getUserSafety(req.getAccount());
        if (req.getStatus() != null && req.getStatus() == 2) {
            if (StringUtils.isBlank(safety.getGoogle())) {
                throw new DcException(BaseResultCodeEnum.BIND_GOOGLE);
            }
        }
        String accountKey = RedisKeyUtil.findPasswordAccountNumber(safety.getUserId());
        Integer accountNumber = RedisUtil.getNumber(accountKey);
        if(accountNumber > 3){
            throw new DcException(BaseResultCodeEnum.RESTRICTIONS_ON_FIND_PASSWORD);
        }

        String ip = IpUtils.getIpAddr(request);
        String[] arrIp = ip.split(",");
        ip = arrIp[arrIp.length - 1];
        String ipKey = RedisKeyUtil.findPasswordIpNumber(ip);
        Integer ipNumber = RedisUtil.getNumber(ipKey);
        if(ipNumber > 3){
            throw new DcException(BaseResultCodeEnum.RESTRICTIONS_ON_FIND_PASSWORD);
        }

        LambdaQueryWrapper<UserSafety> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSafety::getUserId, safety.getUserId());
        UserSafety userSafety = userSafetyService.getOne(wrapper);
        if(req.getStatus() == 1){
            if(!safetyCenterService.verifyMailCode(SmsStatusEnum.FIND_PASSWORD,userSafety.getMail(), req.getVerification())){
                accountNumber++;
                RedisUtil.setEx(accountKey, accountNumber.toString(), 60 * 60 * 24);
                ipNumber++;
                RedisUtil.setEx(ipKey, ipNumber.toString(), 60 * 60 * 24);
                throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
            }
        }else{
            if(!googleAuthenticatorUtil.verifyCode(userSafety.getGoogle(), Integer.parseInt(req.getVerification()))){
                accountNumber++;
                RedisUtil.setEx(accountKey, accountNumber.toString(), 60 * 60 * 24);
                ipNumber++;
                RedisUtil.setEx(ipKey, ipNumber.toString(), 60 * 60 * 24);
                throw new DcException(BaseResultCodeEnum.GOOGLE_VERIFICATION_FAILED);
            }
        }
        RedisUtil.del(accountKey);
        RedisUtil.del(ipKey);
        return userSafety.getPasswordLogin();
    }

    @Login
    @PostMapping(value = "/modifyAccount")
    @SecureRequest
    @Operation(summary = "修改账号")
    public Map<String, Boolean> modifyAccount(@RequestBody @Validated AccountModifyReq req) {
        String account = req.getAccount().toLowerCase();
        String regex = "^(?=.*[0-9])(?=.*[a-zA-Z])[a-zA-Z0-9]{8,15}$";

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getAccount, account);
        User one = userService.getOne(userWrapper);
        if(one != null){
            throw new DcException(BaseResultCodeEnum.ACCOUNT_ALREADY_EXIST);
        }
        if (!account.matches(regex)) {
            throw new DcException(BaseResultCodeEnum.ACCOUNT_FORMAT_ERROR);
        }

        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        String key = RedisKeyUtil.modifyAccountNumber(memberId);
        Integer number = RedisUtil.getNumber(key);
        if(number > 5){
            throw new DcException(BaseResultCodeEnum.RESTRICTIONS_ON_MODIFY_ACCOUNT);
        }

        LambdaQueryWrapper<UserSafety> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSafety::getUserId, memberId);
        UserSafety userSafety = userSafetyService.getOne(wrapper);
        if(req.getStatus() == 1){
            if(!safetyCenterService.verifyMail(SmsStatusEnum.MODIFY_ACCOUNT,userSafety.getMail(), req.getVerification())){
                number++;
                RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
                throw new DcException(BaseResultCodeEnum.EMAIL_VERIFICATION_FAILED);
            }
        }else{
            if(!googleAuthenticatorUtil.verifyCode(userSafety.getGoogle(), Integer.parseInt(req.getVerification()))){
                number++;
                RedisUtil.setEx(key,number.toString(), 60 * 60 * 12);
                throw new DcException(BaseResultCodeEnum.GOOGLE_VERIFICATION_FAILED);
            }
        }
        RedisUtil.del(key);
        User user = userService.getById(memberId);
        user.setAccount(account);
        userService.updateById(user);
        return Collections.singletonMap("result",true);
    }

    private UserSafety getUserSafety(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFlag,1).eq(User::getAccount, account.toLowerCase());
        User user = userService.getOne(wrapper);

        LambdaQueryWrapper<UserSafety> safetyLambdaQueryWrapper = new LambdaQueryWrapper<>();
        safetyLambdaQueryWrapper.eq(UserSafety::getFlag, 1).eq(UserSafety::getMail, account.toLowerCase());
        List<UserSafety> userSafety = userSafetyService.list(safetyLambdaQueryWrapper);
        if(user == null && (userSafety.isEmpty())){
            throw new DcException(BaseResultCodeEnum.THE_ACCOUNT_DOES_NOT_EXIST);
        }
        if (userSafety.size() > 1) {
            throw new DcException(BaseResultCodeEnum.MAIL_BIND_MULTIPLE_ACCOUNT);
        }
        UserSafety safety = null;
        if(user == null){
            safety = userSafety.get(0);
        } else {
            safety = userSafetyService.getById(user.getId());
        }
        if (StringUtils.isBlank(safety.getMail()) && StringUtils.isBlank(safety.getGoogle())) {
            throw new DcException(BaseResultCodeEnum.NO_BIND_GOOGLE_AND_EMIAL);
        }
        return safety;
    }

    private String maskString(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str.substring(0, 2)); // 前两个字符

        for (int i = 2; i < str.length() - 2; i++) {
            sb.append('*');
        }

        sb.append(str.substring(str.length() - 2)); // 最后两个字符
        return sb.toString();
    }
}
