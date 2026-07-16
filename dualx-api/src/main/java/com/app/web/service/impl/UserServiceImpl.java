package com.app.web.service.impl;

import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.IpUtils;
import com.app.common.util.RedisUtil;
import com.app.db.entity.User;
import com.app.db.entity.UserSafety;
import com.app.db.entity.UserWallet;
import com.app.db.entity.Vo.InvitationCodeVo;
import com.app.db.entity.Vo.UserVo;
import com.app.db.mapper.UserMapper;
import com.app.web.api.req.AccountSignUpReq;
import com.app.web.api.resp.RegisterResp;
import com.app.web.service.IUserSafetyService;
import com.app.web.service.IUserService;
import com.app.web.service.IUserWalletService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private IUserWalletService walletService;

    @Resource
    private IUserSafetyService userSafetyService;

    @Value(value = "${com.app.userNumber}")
    private Integer userNumber;

    @Override
    public List<Integer> splitAllPid(String allPid) {
        if (StringUtils.isBlank(allPid)) {
            return Collections.emptyList();
        }
        String[] arr = allPid.split(",");
        List<Integer> res = new ArrayList<>(arr.length);
        for (String str : arr) {
            if (NumberUtils.isNumber(str)) {
                res.add(Integer.parseInt(str));
            }
        }
        return res;
    }

    @Override
    public User fetchOne(int memberId) {
        return getById(memberId);
    }

    @Override
    public User fetchOneByUid(String uid) {
        if (StringUtils.isBlank(uid)) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFlag, 1).eq(User::getUid, uid);
        return getOne(wrapper);
    }

    @Override
    public void checkPassword(int memberId, String password) {
        UserSafety safety = userSafetyService.getById(memberId);
        if (safety == null || !Objects.equals(safety.getPasswordPay(), password)) {
            throw new DcException(BaseResultCodeEnum.PAYMENT_PASSWORD_ERROR);
        }
    }

    @Override
    public InvitationCodeVo invitationCode() {
        int memberId = Integer.parseInt(com.app.common.util.RequestUtil.getCurrentAccount());
        User memberRecord = getById(memberId);
        return memberRecord != null
            ? new InvitationCodeVo().setInvitationCode(memberRecord.getShareCode())
            : new InvitationCodeVo();
    }

    @Override
    public RegisterResp signUp(AccountSignUpReq req, HttpServletRequest request) {
        RegisterResp registerResp = new RegisterResp();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFlag, 1).eq(User::getAccount, req.getAccount());
        if (count(wrapper) > 0) {
            throw new DcException(BaseResultCodeEnum.ACCOUNT_ALREADY_EXIST);
        }

        String ip = IpUtils.getIpAddr(request);
        String[] arrIp = ip.split(",");
        ip = arrIp[arrIp.length - 1];
        Integer number = RedisUtil.getNumber(ip);
        number = number + 1;
        if (number > userNumber) {
            throw new DcException(BaseResultCodeEnum.REGISTRATION_FAILED);
        }
        RedisUtil.setEx(ip, number + "", 60 * 60);

        User referMember = null;
        if (StringUtils.isNotBlank(req.getShareCode())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getFlag, 1).eq(User::getShareCode, req.getShareCode().toUpperCase());
            referMember = getOne(wrapper);
            if (referMember == null) {
                throw new DcException(BaseResultCodeEnum.INVALID_INVITATION_CODE);
            }
        }

        String shareCode = RandomStringUtils.random(6, true, true).toUpperCase(Locale.ROOT);
        while (true) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getShareCode, shareCode);
            if (count(wrapper) == 0) {
                break;
            }
            shareCode = RandomStringUtils.random(6, true, true).toUpperCase(Locale.ROOT);
        }

        String uid = "1" + RandomStringUtils.random(7, false, true).toUpperCase(Locale.ROOT);
        while (true) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUid, uid);
            if (count(wrapper) == 0) {
                break;
            }
            uid = "1" + RandomStringUtils.random(7, false, true).toUpperCase(Locale.ROOT);
        }

        User memberRecord = new User();
        memberRecord.setAccount(req.getAccount());
        if (referMember != null) {
            memberRecord.setReferId(referMember.getId());
        }
        memberRecord.setChainType(req.getChainType());
        memberRecord.setUid(uid);
        memberRecord.setShareCode(shareCode);
        memberRecord.setNumberOfSubordinates(0);
        memberRecord.setUserImg(RandomUtils.nextInt(1, 9) + "");
        memberRecord.setUserName("RWA" + memberRecord.getShareCode());
        save(memberRecord);

        if (referMember != null) {
            memberRecord.setTeamAllPid(referMember.getTeamAllPid() + memberRecord.getId() + ",");
        } else {
            memberRecord.setTeamAllPid("," + memberRecord.getId() + ",");
        }

        if (referMember != null) {
            List<Integer> allPids = splitAllPid(memberRecord.getTeamAllPid());
            UpdateWrapper<User> teamNumUpdateWrapper = new UpdateWrapper<>();
            teamNumUpdateWrapper.in("id", allPids).setSql("team_num = team_num + 1");
            update(teamNumUpdateWrapper);
            updateById(new User().setId(referMember.getId()).setShareNum(referMember.getShareNum() + 1));
            teamNumUpdateWrapper = new UpdateWrapper<>();
            teamNumUpdateWrapper.eq("id", referMember.getId()).setSql("number_of_subordinates = number_of_subordinates + 1");
            update(teamNumUpdateWrapper);
        } else {
            UpdateWrapper<User> teamNumUpdateWrapper = new UpdateWrapper<>();
            teamNumUpdateWrapper.eq("id", memberRecord.getId()).setSql("team_num = team_num + 1");
            update(teamNumUpdateWrapper);
        }

        registerResp.setAccount(memberRecord.getAccount());
        registerResp.setPassword(req.getPassword());

        if (referMember != null) {
            memberRecord.setTeamAllPid(referMember.getTeamAllPid() + memberRecord.getId() + ",");
        } else {
            memberRecord.setTeamAllPid("," + memberRecord.getId() + ",");
        }
        memberRecord.setShareNum(null);
        updateById(memberRecord);
        walletService.initWallet(memberRecord.getId(), req.getAccount());

        UserSafety userSafety = new UserSafety();
        userSafety.setId(memberRecord.getId());
        userSafety.setUserId(memberRecord.getId());
        userSafety.setStatus(1);
        userSafety.setFlag(1);
        userSafety.setPasswordLogin(req.getPassword());
        userSafety.setPasswordPay("");
        userSafetyService.save(userSafety);
        return registerResp;
    }

    @Override
    public void findUser(UserVo userVo) {
        LambdaQueryWrapper<UserWallet> userWalletLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userWalletLambdaQueryWrapper.eq(UserWallet::getMemberId, userVo.getId());
        List<UserWallet> wallets = walletService.list(userWalletLambdaQueryWrapper);
        for (UserWallet wallet : wallets) {
            wallet.setBalance(wallet.getBalance().setScale(2, RoundingMode.DOWN));
            wallet.setFrozen(wallet.getFrozen().setScale(2, RoundingMode.DOWN));
        }
        userVo.setUserWallets(wallets);
    }

    @Override
    public User findUserByAccount(String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFlag, 1).eq(User::getAccount, account.toLowerCase().trim());
        return getOne(wrapper);
    }
}
