package com.app.common.util;


import java.util.HashMap;
import java.util.Map;

/**
 * 币种工具类
 */
public class CoinUtil {

    public static Map<String,Integer> coinMap = new HashMap<>();

    public static String RWA = "RWA";

    public static String WX = "WX";

    public static Map<String,Integer> getCoinMap(){
        if(coinMap.isEmpty()){
            //积分
            coinMap.put(WX,1);
            //代币
            coinMap.put(RWA,2);
        }
        return coinMap;
    }

}
