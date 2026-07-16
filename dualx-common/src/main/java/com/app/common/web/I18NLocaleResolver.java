package com.app.common.web;


import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class I18NLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest httpServletRequest) {
        //获取请求中的语言参数
        String language = httpServletRequest.getHeader("Accept-Language");
        Locale locale;
        if (StringUtils.isNotBlank(language) && language.contains("_")) {
            // 如果请求头中携带了国际化的参数，创建对应的 Locale 对象
            String[] strings = language.split("_");
            locale = Locale.of(strings[0], strings[1]);
        } else {
            //如果没有，使用默认的 Locale 对象（根据主机的语言环境生成一个 Locale ）。
            locale = Locale.getDefault();
        }
        LocaleContextHolder.setLocale(locale);
        return locale;
    }

    @Override
    public void setLocale(HttpServletRequest httpServletRequest,
                          HttpServletResponse httpServletResponse, Locale locale) {

    }

    private static final Map<String, String> COUNTRY_TO_LANG = new HashMap<>();

    static {
        // 国家代码到语言标签的映射
        COUNTRY_TO_LANG.put("CN", "zh_CN"); // 中国 -> 简体中文
        COUNTRY_TO_LANG.put("US", "en_US"); // 美国 -> 英文(美国)
        COUNTRY_TO_LANG.put("VN", "vi_VN"); // 越南 -> 越语
        COUNTRY_TO_LANG.put("RU", "ru_RU"); // 俄罗斯 -> 俄文
        COUNTRY_TO_LANG.put("KR", "ko_KR"); // 韩国 -> 韩文
        COUNTRY_TO_LANG.put("PT", "pt_PT"); // 葡萄牙 -> 葡文
        // 添加更多映射...
    }

    public static String getLanguageTag(String countryCode) {
        // 默认语言标签
        String defaultLang = "zh_CN";

        if (countryCode == null || countryCode.isEmpty()) {
            return defaultLang;
        }
        return COUNTRY_TO_LANG.getOrDefault(countryCode.toUpperCase(), defaultLang);
    }

    public static Locale getLocale(String countryCode) {
        String langTag = getLanguageTag(countryCode);
        return Locale.forLanguageTag(langTag);
    }
}
