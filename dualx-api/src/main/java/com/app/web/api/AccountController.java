package com.app.web.api;

import cn.dev33.satoken.stp.StpUtil;
import com.app.common.annotation.Login;
import com.app.common.annotation.SecureRequest;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.*;
import com.app.common.web.I18NLocaleResolver;
import com.app.db.entity.User;
import com.app.db.entity.UserSafety;
import com.app.db.entity.Vo.UserPasswordVo;
import com.app.db.entity.Vo.UserVo;
import com.app.web.api.req.AccountSignInReq;
import com.app.web.api.req.AccountSignUpReq;
import com.app.web.api.resp.AccountSignInResp;
import com.app.web.api.resp.RegisterResp;
import com.app.web.service.IUserSafetyService;
import com.app.web.service.IUserService;
import com.app.web.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/api/account")
@Tag(name = "账号相关")
@CrossOrigin(origins = "*")
public class AccountController {

    @Resource
    private IUserService userService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private IUserSafetyService userSafetyService;

    @Resource
    private RestTemplate restTemplate;

    private static final String API_URL = "http://ip-api.com/json/%s?fields=countryCode";

    @GetMapping(value = "/exist")
    @Operation(summary = "检查账号是否注册")
    public Map<String, Boolean> exist(String account) {
        if (StringUtils.isBlank(account)) {
            throw new DcException(BaseResultCodeEnum.NO_ACCOUNT_DETECTED);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFlag, 1).eq(User::getAccount, account.toLowerCase().trim());
        return Collections.singletonMap("exist", userService.count(wrapper) > 0);
    }

    @PostMapping(value = "/signUp")
    @SecureRequest
    @Operation(summary = "注册")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public RegisterResp signUp(@RequestBody @Validated AccountSignUpReq req, HttpServletRequest request) {
        String regex = "^(?=.*[0-9])(?=.*[a-zA-Z])[a-zA-Z0-9]{8,15}$";
        if (!req.getAccount().matches(regex)) {
            throw new DcException(BaseResultCodeEnum.ACCOUNT_FORMAT_ERROR);
        }
        if (StringUtils.isBlank(req.getPassword()) || req.getPassword().length() < 8 || req.getPassword().length() > 30) {
            throw new DcException(BaseResultCodeEnum.PASSWORD_FORMAT_ERROR8_TO30);
        }
        String value = sysConfigService.getStringValue("LOIN_IMINT");
        if ("0".equals(value)) {
            throw new DcException(BaseResultCodeEnum.REGISTRATION_SUSPENSION);
        }
        req.setAccount(req.getAccount().trim().toLowerCase());
        if (!RedisUtil.tryLock("AccountController.signUp." + req.getAccount(), 3)) {
            throw new DcException(BaseResultCodeEnum.REGISTRATION_IS_TOO_FREQUENT);
        }
        log.info(">>> 新用户注册 - {}", JsonUtil.toJson(req));
        return userService.signUp(req, request);
    }

    @PostMapping(value = "/signIn")
    @SecureRequest
    @Operation(summary = "登录")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public AccountSignInResp signIn(@RequestBody @Validated AccountSignInReq req) {
        if (StringUtils.isBlank(req.getAccount()) || StringUtils.isBlank(req.getPassword())) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        req.setAccount(req.getAccount().trim().toLowerCase());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFlag, 1).eq(User::getAccount, req.getAccount());
        User memberRecord = userService.getOne(wrapper);

        LambdaQueryWrapper<UserSafety> safetyLambdaQueryWrapper = new LambdaQueryWrapper<>();
        safetyLambdaQueryWrapper.eq(UserSafety::getFlag, 1).eq(UserSafety::getMail, req.getAccount());
        List<UserSafety> userSafetyList = userSafetyService.list(safetyLambdaQueryWrapper);
        if (memberRecord == null && CollectionUtils.isEmpty(userSafetyList)) {
            throw new DcException(BaseResultCodeEnum.THE_ACCOUNT_DOES_NOT_EXIST);
        }
        if (!CollectionUtils.isEmpty(userSafetyList) && userSafetyList.size() > 1) {
            throw new DcException(BaseResultCodeEnum.MAIL_BIND_MULTIPLE_ACCOUNT_LOGIN);
        }
        User user = memberRecord;
        if (!CollectionUtils.isEmpty(userSafetyList)) {
            user = userService.getById(userSafetyList.get(0).getUserId());
        }
        UserSafety safety = userSafetyService.getById(user.getId());
        String key = RedisKeyUtil.logInPasswordNumber(user.getId());
        if (!Md5Util.md5(safety.getPasswordLogin().toLowerCase()).equals(req.getPassword())
            && !Md5Util.md5(safety.getPasswordLogin()).equals(req.getPassword())) {
            Integer number = RedisUtil.getNumber(key) + 1;
            if (number > 6) {
                throw new DcException(BaseResultCodeEnum.PASSWORD_ERR_LIMIT);
            }
            RedisUtil.setEx(key, number + "", 60 * 30);
            throw new DcException(BaseResultCodeEnum.ERROR_PASSWORD);
        }
        RedisUtil.del(key);
        StpUtil.login(user.getId());
        AccountSignInResp resp = new AccountSignInResp();
        resp.setToken(StpUtil.getTokenValue());
        return resp;
    }

    @Login
    @GetMapping(value = "/findPasswordPay")
    @Operation(summary = "查询用户是否设置支付密码")
    public Map<String, Boolean> findPasswordPay() {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        UserSafety user = userSafetyService.getById(memberId);
        return Collections.singletonMap("exist", StringUtils.isNotBlank(user.getPasswordPay()));
    }

    @Login
    @GetMapping(value = "/findUser")
    @Operation(summary = "查询用户个人信息")
    public UserVo findUser() {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        User user = userService.getById(memberId);
        UserVo userVo = new UserVo();
        userVo.setId(memberId);
        userVo.setAccount(user.getAccount());
        userVo.setUserName(user.getUserName());
        userVo.setUserTime(user.getUserTime());
        userVo.setUid(user.getUid());
        userVo.setUserImg(user.getUserImg());
        userVo.setShareCode(user.getShareCode());
        userVo.setLevel(user.getLevel());
        userVo.setLevelStatus(user.getLevelStatus());
        userService.findUser(userVo);
        return userVo;
    }

    @Login
    @GetMapping(value = "/findUserNameOrPassword")
    @Operation(summary = "查询用户账号密码")
    public UserPasswordVo findUserNameOrPassword() {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        User user = userService.getById(memberId);
        UserSafety safety = userSafetyService.getById(memberId);
        UserPasswordVo userPasswordVo = new UserPasswordVo();
        userPasswordVo.setAccount(user.getAccount());
        userPasswordVo.setPassword(safety.getPasswordLogin());
        String count = "account=" + user.getAccount() + "&password=" + safety.getPasswordLogin();
        userPasswordVo.setBase64Img(QRCodeUtil.generateQRCodeBase64(count, 300, 300));
        return userPasswordVo;
    }

    @Login
    @GetMapping(value = "/modifyTheName")
    @Operation(summary = "修改名称")
    public Map<String, Boolean> modifyTheName(String userName) {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        return Collections.singletonMap("result", userService.updateById(new User().setId(memberId).setUserName(userName)));
    }

    @Login
    @GetMapping(value = "/modifyImage")
    @Operation(summary = "修改头像")
    public Map<String, Boolean> modifyImage(@RequestParam String imageUrl) {
        int memberId = Integer.parseInt(RequestUtil.getCurrentAccount());
        return Collections.singletonMap("result", userService.updateById(new User().setId(memberId).setUserImg(imageUrl)));
    }

    @GetMapping(value = "/getLanguage")
    @Operation(summary = "获取当前环境语言标识")
    public String getLanguage(HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        String[] arrIp = ip.split(",");
        ip = arrIp[arrIp.length - 1];
        try {
            String url = String.format(API_URL, ip);
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                return I18NLocaleResolver.getLanguageTag(response.get("countryCode"));
            }
        } catch (Exception ignored) {
        }
        return "zh_CN";
    }

    @Login
    @GetMapping(value = "/getUserInfo")
    @Operation(summary = "根据uid查询用户")
    public User getUserInfo(@RequestParam String uid) {
        return userService.fetchOneByUid(uid);
    }
}
