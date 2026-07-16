package com.app.common.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public  class SelectScopeUtil {
/*
* 礼盒返积分比例
* */
    public static BigDecimal getRateAmount(double amount ) {
        BigDecimal count = new BigDecimal(0);
        if (1 <= amount && amount <= 100) {
            count = SelectScopeUtil.count1to100(new BigDecimal(amount), new BigDecimal(2));
        } else if (101 <= amount && amount <= 500) {
            count = SelectScopeUtil.count101to500(new BigDecimal(amount).subtract(new BigDecimal(100)), new BigDecimal("2.1"));
        } else if (501 <= amount && amount <= 2000) {
            count = SelectScopeUtil.count501to2000(new BigDecimal(amount).subtract(new BigDecimal(500)), new BigDecimal("2.2"));
        } else if (2001 <= amount && amount <= 5000) {
            count = SelectScopeUtil.count2001to5000(new BigDecimal(amount).subtract(new BigDecimal(2000)), new BigDecimal("2.3"));
        } else if (5001 <= amount && amount <= 10000) {
            count = SelectScopeUtil.count5001to10000(new BigDecimal(amount).subtract(new BigDecimal(5000)), new BigDecimal("2.4"));
        } else if (amount > 10000) {
            count = SelectScopeUtil.countTo10000(new BigDecimal(amount).subtract(new BigDecimal(10000)), new BigDecimal("2.5"));
        }
        return count;
    }

    public static  BigDecimal countTo10000( BigDecimal amount,BigDecimal rate){
        return amount.multiply(rate).add(SelectScopeUtil.count5001to10000(new BigDecimal(5000),new BigDecimal("2.4")));
    }
    public static  BigDecimal count5001to10000( BigDecimal amount,BigDecimal rate){
        return amount.multiply(rate).add(SelectScopeUtil.count2001to5000(new BigDecimal(3000),new BigDecimal("2.3")));
    }
    public static  BigDecimal count2001to5000( BigDecimal amount,BigDecimal rate){
        return amount.multiply(rate).add(SelectScopeUtil.count501to2000(new BigDecimal(1500),new BigDecimal("2.2")));
    }
    public static  BigDecimal count501to2000( BigDecimal amount,BigDecimal rate){
        return amount.multiply(rate).add(SelectScopeUtil.count101to500(new BigDecimal(400),new BigDecimal("2.1")));
    }
    public static  BigDecimal count101to500( BigDecimal amount,BigDecimal rate){
        return amount.multiply(rate).add(SelectScopeUtil.count1to100(new BigDecimal(100),new BigDecimal("2")));
    }
       public static  BigDecimal count1to100( BigDecimal amount,BigDecimal rate){
        return amount.multiply(rate);
        }
    public static Map<String, BigDecimal> getBrokerRights(Integer leve) {
        Map<String, BigDecimal> map = new HashMap<>();
        switch (leve) {
            /*3为1星经纪人*/
            case 3:
                map.put("equity", new BigDecimal("0.05"));
                map.put("ping", new BigDecimal("0.2"));
                break;
            case 4:
                map.put("equity", new BigDecimal("0.1"));
                map.put("ping", new BigDecimal("0.2"));
                break;
            case 5:
                map.put("equity", new BigDecimal("0.15"));
                map.put("ping", new BigDecimal("0.2"));
                break;
            case 6:
                map.put("equity", new BigDecimal("0.2"));
                map.put("ping", new BigDecimal("0.2"));
                break;
            case 7:
                map.put("equity", new BigDecimal("0.25"));
                map.put("ping", new BigDecimal("0.2"));
                break;

            default:
                map.put("equity", BigDecimal.ZERO);
                map.put("ping", BigDecimal.ZERO);


    }
        return map;
    }

    }


