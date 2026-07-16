package com.app.common.util;


import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


public class IntegerUtil {

    public static Integer getInteger(String s,Integer value){
        return StringUtils.isBlank(s) ? value : Integer.parseInt(s);
    }
}
