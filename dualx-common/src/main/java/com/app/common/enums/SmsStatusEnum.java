package com.app.common.enums;

/**
 * 短信枚举类
 */
public enum SmsStatusEnum implements BaseEnum{
    //绑定
    ADD_MAIL(1, "ADD_MAIL","您正在绑定邮箱,验证码为:%s。请注意不要泄露给他人。","RWA绑定邮箱", "zh_CN"),
    ADD_MAIL_US(1, "ADD_MAIL","You are currently binding your email. The verification code is: %s. Please do not share it with others.","RWA Email Binding", "en_US"),
    ADD_MAIL_KR(1, "ADD_MAIL","귀하는 현재 이메일을 바인딩 중입니다. 인증 코드는 %s입니다. 타인에게 공유하지 마십시오.","RWA 이메일 바인딩", "ko_KR"),
    ADD_MAIL_RU(1, "ADD_MAIL","Вы привязываете электронную почту. Код подтверждения: %s. Пожалуйста, никому не сообщайте его.","Привязка почты в RWA", "ru_RU"),
    ADD_MAIL_VN(1, "ADD_MAIL","Bạn đang thực hiện liên kết email. Mã xác nhận là: %s. Vui lòng không chia sẻ mã này với người khác.","Liên kết email RWA", "vi_VN"),
    ADD_MAIL_PT(1, "ADD_MAIL","Você está vinculando seu e-mail. O código de verificação é: %s. Por favor, não compartilhe com outras pessoas.","Vincular e-mail ao RWA", "pt_PT"),
    ADD_MAIL_NUMBER(2, "ADD_MAIL_NUMBER","",null, null),

    //修改邮箱
    MODIFY_MAIL(3, "MODIFY_MAIL","您正在修改邮箱,验证码为:%s。请注意不要泄露给他人。","RWA修改邮箱", "zh_CN"),
    MODIFY_MAIL_US(3, "MODIFY_MAIL","You are currently updating your email. Verification code: %s. Please do not share this code with anyone.","RWA Modify Email", "en_US"),
    MODIFY_MAIL_KR(3, "MODIFY_MAIL","이메일 변경을 처리 중입니다. 인증번호: %s. 타인에게 공유하지 마십시오.","RWA 이메일 변경", "ko_KR"),
    MODIFY_MAIL_RU(3, "MODIFY_MAIL","Вы изменяете адрес электронной почты. Код подтверждения: %s. Никому не сообщайте этот код.","RWA Изменение email", "ru_RU"),
    MODIFY_MAIL_VN(3, "MODIFY_MAIL","Bạn đang thay đổi email. Mã xác minh: %s. Vui lòng không chia sẻ mã này với người khác.","RWA Thay đổi email", "vi_VN"),
    MODIFY_MAIL_PT(3, "MODIFY_MAIL","Você está alterando seu e-mail. Código de verificação: %s. Não compartilhe este código com ninguém.","RWA Alterar e-mail", "pt_PT"),
    MODIFY_MAIL_NUMBER(4, "MODIFY_MAIL_NUMBER","",null, null),

    //提币
    RWA_WITHDRAWAL(5, "RWA_WITHDRAWAL","您正在进行提币,验证码为:%s。请注意不要泄露给他人。","RWA提币", "zh_CN"),
    RWA_WITHDRAWAL_US(5, "RWA_WITHDRAWAL","You are currently processing a cryptocurrency withdrawal. Verification code: %s. Do not share this code with anyone.","RWA Crypto Withdrawal", "en_US"),
    RWA_WITHDRAWAL_KR(5, "RWA_WITHDRAWAL","현재 암호화폐 출금을 처리 중입니다. 인증번호: %s. 타인에게 공유하지 마십시오.","RWA 코인 출금", "ko_KR"),
    RWA_WITHDRAWAL_RU(5, "RWA_WITHDRAWAL","Вы осуществляете вывод криптовалюты. Код подтверждения: %s. Никому не сообщайте этот код.","RWA Вывод средств", "ru_RU"),
    RWA_WITHDRAWAL_VN(5, "RWA_WITHDRAWAL","Bạn đang thực hiện rút tiền điện tử. Mã xác minh: %s. Vui lòng không chia sẻ mã này với bất kỳ ai.","RWA Rút tiền điện tử", "vi_VN"),
    RWA_WITHDRAWAL_PT(5, "RWA_WITHDRAWAL","Você está realizando um saque de criptomoeda. Código de verificação: %s. Não compartilhe este código com ninguém.","RWA Saque de cripto", "pt_PT"),
    RWA_WITHDRAWAL_NUMBER(6, "RWA_WITHDRAWAL_NUMBER","",null, null),

    //转账
    RWA_TRANSFER(7, "RWA_TRANSFER","您正在进行转账,验证码为:%s。请注意不要泄露给他人。","RWA转账", "zh_CN"),
    RWA_TRANSFER_US(7, "RWA_TRANSFER","You are currently making a transfer. The verification code is: %s. Please do not share it with others.","RWA Transfer", "en_US"),
    RWA_TRANSFER_KR(7, "RWA_TRANSFER","현재 송금을 진행 중입니다. 인증번호는 %s입니다. 타인에게 공유하지 마십시오.","RWA 송금", "ko_KR"),
    RWA_TRANSFER_RU(7, "RWA_TRANSFER","Вы совершаете перевод. Код подтверждения: %s. Пожалуйста, никому его не сообщайте.","RWA Перевод", "ru_RU"),
    RWA_TRANSFER_VN(7, "RWA_TRANSFER","Bạn đang thực hiện chuyển tiền. Mã xác minh là: %s. Vui lòng không chia sẻ mã này với người khác.","RWA Chuyển tiền", "vi_VN"),
    RWA_TRANSFER_PT(7, "RWA_TRANSFER","Você está realizando uma transferência. O código de verificação é: %s. Por favor, não compartilhe com outras pessoas.","RWA Transferência", "pt_PT"),
    RWA_TRANSFER_NUMBER(8, "RWA_TRANSFER_NUMBER","",null, null),

    //修改登录密码
    MODIFY_THE_LOGIN_PASSWORD(9, "MODIFY_THE_LOGIN_PASSWORD","您正在修改登录密码,验证码为:%s。请注意不要泄露给他人。","RWA修改登录密码", "zh_CN"),
    MODIFY_THE_LOGIN_PASSWORD_US(9, "MODIFY_THE_LOGIN_PASSWORD","You are changing your login password. The verification code is: %s. Please do not share it with others.","RWA Change Login Password", "en_US"),
    MODIFY_THE_LOGIN_PASSWORD_KR(9, "MODIFY_THE_LOGIN_PASSWORD","로그인 비밀번호를 변경 중입니다. 인증번호는 %s입니다. 타인에게 공유하지 마십시오.","RWA 로그인 비밀번호 변경", "ko_KR"),
    MODIFY_THE_LOGIN_PASSWORD_RU(9, "MODIFY_THE_LOGIN_PASSWORD","Вы изменяете пароль для входа. Код подтверждения: %s. Пожалуйста, никому его не сообщайте.","Смена пароля для входа в RWA", "ru_RU"),
    MODIFY_THE_LOGIN_PASSWORD_VN(9, "MODIFY_THE_LOGIN_PASSWORD","Bạn đang thay đổi mật khẩu đăng nhập. Mã xác minh là: %s. Vui lòng không chia sẻ mã này với người khác.","RWA Đổi mật khẩu đăng nhập", "vi_VN"),
    MODIFY_THE_LOGIN_PASSWORD_PT(9, "MODIFY_THE_LOGIN_PASSWORD","Você está alterando sua senha de login. O código de verificação é: %s. Por favor, não compartilhe com outras pessoas.","RWA Alterar Senha de Login", "pt_PT"),
    MODIFY_THE_LOGIN_PASSWORD_NUMBER(10, "MODIFY_THE_LOGIN_PASSWORD_NUMBER","",null, null),

    //注册
    USER_REGISTER(11, "USER_REGISTER","您正在注册,验证码为:%s。请注意不要泄露给他人。","RWA注册", "zh_CN"),
    USER_REGISTER_US(11, "USER_REGISTER","You are registering, the verification code is: %s. Please do not share it with others.","RWA Registration", "en_US"),
    USER_REGISTER_KR(11, "USER_REGISTER","회원가입 중입니다. 인증번호는 %s입니다. 타인에게 공유하지 마시기 바랍니다.","RWA 회원가입", "ko_KR"),
    USER_REGISTER_RU(11, "USER_REGISTER","Вы проходите регистрацию. Код подтверждения: %s. Пожалуйста, никому его не сообщайте.","Регистрация в RWA", "ru_RU"),
    USER_REGISTER_VN(11, "USER_REGISTER","Bạn đang đăng ký tài khoản. Mã xác thực là: %s. Vui lòng không chia sẻ mã này với người khác.","Đăng ký RWA", "vi_VN"),
    USER_REGISTER_PT(11, "USER_REGISTER","Você está se registrando. O código de verificação é: %s. Por favor, não compartilhe com outras pessoas.","Cadastro no RWA", "pt_PT"),
    USER_REGISTER_NUMBER(12, "USER_REGISTER_NUMBER","",null, null),

    //修改登录账号
    MODIFY_ACCOUNT(13, "MODIFY_ACCOUNT","您正在修改登录账号,验证码为:%s。请注意不要泄露给他人。","RWA修改登录账号", "zh_CN"),
    MODIFY_ACCOUNT_US(13, "MODIFY_ACCOUNT","You are changing your login account. The verification code is: %s. Please do not share it with others.","RWA Modify Login account", "en_US"),
    MODIFY_ACCOUNT_KR(13, "MODIFY_ACCOUNT","로그인 계정을 수정 중입니다. 인증번호: %s. 타인에게 공유하지 마세요.","RWA: 로그인 계정 변경", "ko_KR"),
    MODIFY_ACCOUNT_RU(13, "MODIFY_ACCOUNT","Вы изменяете учетную запись. Код подтверждения: %s. Никому не сообщайте его.","RWA: Изменение учетной записи", "ru_RU"),
    MODIFY_ACCOUNT_VN(13, "MODIFY_ACCOUNT","Bạn đang thay đổi tài khoản đăng nhập. Mã xác minh: %s. Không tiết lộ cho người khác.","RWA: Thay đổi tài khoản đăng nhập", "vi_VN"),
    MODIFY_ACCOUNT_PT(13, "MODIFY_ACCOUNT","Você está modificando sua conta. Código de verificação: %s. Não compartilhe com outros.","RWA: Alterar conta de login", "pt_PT"),
    MODIFY_ACCOUNT_NUMBER(14, "MODIFY_ACCOUNT_NUMBER","",null, null),

    //找回登录密码
    FIND_PASSWORD(15, "FIND_PASSWORD","您正在找回登录密码,验证码为:%s。请注意不要泄露给他人。","RWA找回登录密码", "zh_CN"),
    FIND_PASSWORD_US(15, "FIND_PASSWORD","You are resetting your password. The verification code is: %s. Please do not share it with others.","RWA: Recover Login Password", "en_US"),
    FIND_PASSWORD_KR(15, "FIND_PASSWORD","비밀번호를 재설정 중입니다. 인증번호: %s. 타인에게 공유하지 마세요.","RWA: 로그인 비밀번호 찾기", "ko_KR"),
    FIND_PASSWORD_RU(15, "FIND_PASSWORD","Вы восстанавливаете пароль. Код подтверждения: %s. Никому не сообщайте этот код.","RWA: Восстановление пароля", "ru_RU"),
    FIND_PASSWORD_VN(15, "FIND_PASSWORD","Bạn đang khôi phục mật khẩu. Mã xác thực: %s. Không tiết lộ mã này cho người khác.","RWA: Lấy lại mật khẩu", "vi_VN"),
    FIND_PASSWORD_PT(15, "FIND_PASSWORD","Redefinição de senha em andamento. Código: %s. Não compartilhe com terceiros.","RWA: Redefinição de senha", "pt_PT"),
    FIND_PASSWORD_NUMBER(16, "FIND_PASSWORD_NUMBER","",null, null),

    //找回登录密码
    MERCHANT_PAY(17, "MERCHANT_PAY","您正在进行商家支付,验证码为:%s。请注意不要泄露给他人。","RWA商家支付", "zh_CN"),
    MERCHANT_PAY_US(17, "MERCHANT_PAY","You are making a merchant payment. Your verification code is: %s. Please do not share it with others.","RWA Merchant Payment", "en_US"),
    MERCHANT_PAY_KR(17, "MERCHANT_PAY","가맹점 결제를 진행 중입니다. 인증번호는 %s입니다. 타인에게 공유하지 마십시오.","RWA 가맹점 결제", "ko_KR"),
    MERCHANT_PAY_RU(17, "MERCHANT_PAY","Вы осуществляете платеж продавцу. Ваш проверочный код: %s. Пожалуйста, никому его не сообщайте.","Платеж через RWA для продавцов", "ru_RU"),
    MERCHANT_PAY_VN(17, "MERCHANT_PAY","Bạn đang thực hiện thanh toán cho nhà bán. Mã xác minh là: %s. Vui lòng không chia sẻ với người khác.","Thanh toán RWA cho nhà bán", "vi_VN"),
    MERCHANT_PAY_PT(17, "MERCHANT_PAY","Você está realizando um pagamento ao comerciante. Seu código de verificação é: %s. Por favor, não compartilhe com outras pessoas.","Pagamento RWA para comerciantes", "pt_PT"),
    MERCHANT_PAY_NUMBER(18, "MERCHANT_PAY_NUMBER","",null, null),
    ;
    private final int code;
    private final String remark;
    private final String msg;
    private final String title;
    private final String lang;

    SmsStatusEnum(int code, String remark,String msg,String title, String lang) {
        this.code = code;
        this.remark = remark;
        this.msg = msg;
        this.title = title;
        this.lang = lang;
    }

    public String getTitle() {
        return this.title;
    }

    public String getMsg() {
        return this.msg;
    }

    @Override
    public int getEnumCode() {
        return this.code;
    }

    @Override
    public String i18nKey() {
        return "SmsEnum." + getCode();
    }

    public int getCode() {
        return code;
    }

    public String getRemark() {
        return remark;
    }

    public static SmsStatusEnum valueOf(int enumCode) {
        for (SmsStatusEnum typeEnum : SmsStatusEnum.values()) {
            if (typeEnum.getEnumCode() == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }

    public static SmsStatusEnum valueOf(int enumCode, String lang) {
        for (SmsStatusEnum typeEnum : SmsStatusEnum.values()) {
            if (typeEnum.getEnumCode() == enumCode && typeEnum.lang.equals(lang)) {
                return typeEnum;
            }
        }
        return null;
    }
}
