package com.app.web.service;

import com.app.db.entity.User;
import com.app.db.entity.Vo.InvitationCodeVo;
import com.app.db.entity.Vo.UserVo;
import com.app.web.api.req.AccountSignUpReq;
import com.app.web.api.resp.RegisterResp;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface IUserService extends IService<User> {

    List<Integer> splitAllPid(String allPid);

    User fetchOne(int memberId);

    User fetchOneByUid(String uid);

    void checkPassword(int memberId, String password);

    InvitationCodeVo invitationCode();

    RegisterResp signUp(AccountSignUpReq req, HttpServletRequest request);

    void findUser(UserVo userVo);

    User findUserByAccount(String account);
}
