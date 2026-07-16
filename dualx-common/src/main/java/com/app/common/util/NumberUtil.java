package com.app.common.util;

import java.util.Random;
import java.util.UUID;

public final class NumberUtil {

    private static long lastTimestamp = 0;
    private static int sequence = 0;

    /**
     *
     * @param startLetter
     * @return
     */
    public static String createOrderNo(String startLetter){
        return startLetter + generateOrderNo();
    }

    /**
     * 生成16位唯一性的订单号
     * @return
     */
    public static String getUUIDBy16(){
        //随机生成一位整数
        int random = (int) (Math.random()*9+1);
        String valueOf = String.valueOf(random);
        //生成uuid的hashCode值
        int hashCode = UUID.randomUUID().toString().hashCode();
        //可能为负数
        if(hashCode<0){
            hashCode = - hashCode;
        }
        return valueOf + String.format("%015d", hashCode);
    }

    public static synchronized String generateOrderNo() {
        long timestamp = System.currentTimeMillis();

        if (timestamp == lastTimestamp) {
            sequence++;
        } else {
            sequence = 0;
            lastTimestamp = timestamp;
        }
        return String.format("%013d%03d", timestamp, sequence);
    }

    /**
     * 生成大写字母
     * @param size
     * @return
     */
    public static String randomLetter(int size){
        String keyArr= "";
        char key = 0;
        boolean[] flag=new boolean[26];    //定义一个Boolean型数组，用来除去重复值
        for(int i=0;i<size;i++){     //通过循环为数组赋值
            Random rand=new Random();
            int index;
            do{
                index=rand.nextInt(26);    //随机生成0-25的数字并赋值给index
            }while(flag[index]);    //判断flag值是否为true,如果为true则重新为index赋值
            key=(char) (index+65);        //大写字母的ASCII值为65-90，所以给index的值加上65，使其符合大写字母的ASCII值区间
            flag[index]=true;       //让对应的flag值为true

            keyArr +=key;
        }
        return keyArr;
    }

    public static String generateSmsCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    /**
     * 生成32位编码
     * @return string
     */
    public static String getUUID(){
        String uuid = UUID.randomUUID().toString().trim().replaceAll("-", "");
        return uuid;
    }
}
