package com.app.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class BaseUtil {

    public static String md5(String str){
        StringBuffer hexValue = new StringBuffer();
        try {
            MessageDigest m=MessageDigest.getInstance("MD5");
            char [] charArray=str.toCharArray();
            byte[] byteArray=new byte[charArray.length];
            for (int i = 0; i < charArray.length; i++)
                byteArray[i] = (byte) charArray[i];
            byte[] md5Bytes = m.digest(byteArray);

            for (int i = 0; i < md5Bytes.length; i++){
                int val = ((int) md5Bytes[i]) & 0xff;
                if (val < 16)
                    hexValue.append("0");
                hexValue.append(Integer.toHexString(val));
            }
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return hexValue.toString();
    }

    public static boolean Base_HasValue(Object pValue) {
        Boolean HasValue = true;
        if (pValue == (null) || pValue.equals("")) {
            HasValue = false;
        }
        return HasValue;
    }

    public static boolean Base_HasValue(String pValue) {
        Boolean HasValue = true;
        if (pValue == (null)) {
            HasValue = false;
        } else if ("".equals(pValue)) {
            HasValue = false;
        } else if ("".equals(pValue.trim())) {
            HasValue = false;
        } else if ("null".equals(pValue.trim())) {

        }
        return HasValue;
    }

    public static boolean Base_HasValue(Integer pValue) {
        Boolean HasValue = true;
        if (pValue == (null)) {
            HasValue = false;
        }
        return HasValue;
    }

    public static boolean Base_HasValue(Map pValue) {
        Boolean HasValue = true;
        if (pValue == (null) || pValue.size() < 1) {
            HasValue = false;
        }
        return HasValue;
    }

    public static boolean Base_HasValue(List pValue) {
        Boolean HasValue = true;
        if (pValue == (null) || pValue.size() < 1) {
            HasValue = false;
        }
        return HasValue;
    }

    public static boolean Base_HasValue(Double pValue) {
        Boolean HasValue = true;
        if (pValue == (null)) {
            HasValue = false;
        }
        return HasValue;
    }


    public static String[] Base_Split(String string, String divisionChar) {
        int i = 0;
        StringTokenizer tokenizer = new StringTokenizer(string, divisionChar);

        String[] str = new String[tokenizer.countTokens()];

        while (tokenizer.hasMoreTokens()) {
            str[i] = new String();
            str[i] = tokenizer.nextToken();
            i++;
        }

        return str;
    }

    public static String Base_ValueOf(Object obj) {
        return (obj == null) ? "null" : obj.toString();

    }

    public static String Base_GetDataToMd5(JSONObject jsonObject, String Keys) {
        String[] tmpKeys = Base_Split(Keys, ",");
        String resultMd5 = "";
        for (String key : tmpKeys) {
            String tmpMd5 = md5Hex(Base_ValueOf(jsonObject.get(key)));
            resultMd5 += tmpMd5;
        }
        return resultMd5;
    }

    /**
     * Json 数组按照 key 进行去重
     *
     * @param jsonArray 源Json数组对象
     * @param Keys      按照 key 进行比较 ; 可以采用多个 key 进行比较用 , 作为分隔符。
     * @return
     */
    public static JSONArray Base_DistinctData(JSONArray jsonArray, String Keys) {

        Map<String, Object> map = new HashMap<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String resultMd5 = Base_GetDataToMd5(jsonObject, Keys);
            if (!Base_HasValue(map.get(resultMd5))) {
                map.put(resultMd5, jsonObject);
            }
        }

        JSONArray resultArray = new JSONArray();
        Iterator<Map.Entry<String, Object>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            resultArray.add(entry.getValue());
        }
        return resultArray;
    }

    public static Map Base_DistinctData(Map pMap, String Keys) {
        Iterator it = pMap.keySet().iterator();
        while (it.hasNext()) {
            String key;
            String value;
            key = it.next().toString();
            value = (String) pMap.get(key);
            System.out.println(key + "--" + value);
        }
        return null;
    }

    public static Object Base_getDefValue(Object pValue, Object pDefValue) {
        if (Base_HasValue(pValue)) {
            return pValue;
        } else {
            return pDefValue;
        }
    }

    public static String Base_encodeUTF8(String str) {

        try {
            if (Base_HasValue(str)) {
                str = URLDecoder.decode(str, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return str;
    }

    /**
     * @param pName  name
     * @param pValue value
     * @return sql条件尾串
     */
    public static String getLocSqlText(String pName, String pValue) {
        String sqlText = String.format(" AND %s ", pName);
        sqlText = String.format(sqlText + " = %s  ", "'" + pValue + "'");
        return sqlText;
    }

    /**
     * 将fromList的数据添加到toList
     *
     * @param toArray   目标list
     * @param fromArray 数据源
     * @return
     */
    public static JSONArray copyJsonArray(JSONArray toArray, JSONArray fromArray) {

        if (Base_HasValue(fromArray)) {
            for (int i = 0; i < fromArray.size(); i++) {
                JSONObject o = fromArray.getJSONObject(i);
                toArray.add(o);
            }
        }

        return toArray;
    }

    /**
     * 将fromList的数据添加到toList
     *
     * @param toList   目标list
     * @param fromList 数据源
     * @return
     */
    public static List copyList(List toList, List fromList) {

        if (Base_HasValue(fromList)) {
            for (Object o : fromList) {
                fromList.add(o);
            }
        }

        return toList;
    }

    public static void removeDuplicate(List list) {
        HashSet h = new HashSet(list);
        list.clear();
        list.addAll(h);
    }

    public static Object getMinKey(Map<Double, Object> map) {
        if (map == null) return null;
        Set<Double> set = map.keySet();
        Object[] obj = set.toArray();
        Arrays.sort(obj);
        return obj[0];
    }

    public static void Base_CheckHasValue(String Msg, Object pValue) {
        if (!Base_HasValue(pValue)) {
            throw new Error(Msg + " : 是空值");
        }
    }

    public static void Base_CheckHasValue(String Msg, String pValue) {
        if (!Base_HasValue(pValue)) {
            throw new Error(Msg + " : 是空值");
        }
    }

    public static void Base_CheckHasValue(String Msg, Integer pValue) {
        if (!Base_HasValue(pValue)) {
            throw new Error(Msg + " : 是空值");
        }
    }

    public static void Base_CheckHasValue(String Msg, Map pValue) {
        if (!Base_HasValue(pValue)) {
            throw new Error(Msg + " : 是空值");
        }
    }

    public static void Base_CheckHasValue(String Msg, List pValue) {
        if (!Base_HasValue(pValue)) {
            throw new Error(Msg + " : 是空值");
        }
    }

    public static void Base_CheckHasValue(String Msg, Double pValue) {
        if (!Base_HasValue(pValue)) {
            throw new Error(Msg + " : 是空值");
        }
    }

    /**
     * 以减法实现的除法
     *
     * @param denNum 分母
     * @param molNum 分子
     */
    public static int subToDiv(double denNum, double molNum) {
        int result = 0;
        boolean isCheck = false;
        while (!isCheck) {
            if (denNum < molNum) {
                isCheck = true;
            } else {
                result++;
                denNum = denNum - molNum;
            }
        }
        return result;
    }

    /**
     * 以减法实现的取余
     *
     * @param denNum 分母
     * @param molNum 分子
     */
    public static double subToRem(double denNum, double molNum) {
        boolean isCheck = false;
        while (!isCheck) {
            if (denNum < molNum) {
                isCheck = true;
            } else {
                denNum = denNum - molNum;
            }
        }
        return denNum;
    }

    /**
     * 将double格式化为指定小数位的String，不足小数位用0补全
     *
     * @param v     需要格式化的数字
     * @param scale 小数点后保留几位
     * @return
     */
    public static String roundByScale(double v, int scale) throws Exception {
        if (scale < 0) {
            throw new Exception("保留小数点位数不能为零");

        }
        if (scale == 0) {
            return new DecimalFormat("0").format(v);
        }
        String formatStr = "0.";
        for (int i = 0; i < scale; i++) {
            formatStr = formatStr + "0";
        }
        return new DecimalFormat(formatStr).format(v);

    }

    /**
     * Description 去掉前后逗号和空格
     */
    public static String removeComma(String str) {
        String regex = "^(,|，)*|(,|，)*$";
        str = str.trim();
        return str.replaceAll(regex, "");
    }

    /**
     * Description 去掉前后横线和空格
     */
    public static String removeAcross(String str) {
        String regex = "^(-|——)*|(-|——)*$";
        str = str.trim();
        return str.replaceAll(regex, "");
    }

    public static String encodeStr(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    public static String encodeStr1(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String dcodeStr(String str) {
        if (null != str && !StringUtils.isEmpty(str)) {
            try {
                //不匹配%后面两位为数字或字母（包括大小写）的字符
                str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                str = URLDecoder.decode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return str;
        }
        return str;
    }

    public static void decodeJsonObject(JSONObject jsonObject) {
        // 解码,暂时用的
        Set<String> it = jsonObject.keySet();
        if (BaseUtil.Base_HasValue(it)) {
            for (String key : it) {
                Object obj = jsonObject.get(key);
                if (obj instanceof String) {
                    try {
                        jsonObject.put(key, BaseUtil.dcodeStr(jsonObject.getString(key)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void encodeJsonObject(JSONObject jsonObject, String encodeColumn) {
        if (Base_HasValue(encodeColumn)) {
            String arr[] = removeComma(encodeColumn).split(",");
            Set<String> it = jsonObject.keySet();
            if (BaseUtil.Base_HasValue(it)) {
                for (String key : it) {
                    if (Arrays.asList(arr).contains(key)) {
                        Object obj = jsonObject.get(key);
                        if (Base_HasValue(obj)) {
                            if (obj instanceof String) {
                                String data = jsonObject.getString(key);
                                // 转码后的值
                                String encodeData = "";
                                try {
                                    if (data.startsWith("*")) {
                                        data = StringUtils.strip(data, "*");
                                        encodeData = BaseUtil.encodeStr(data);
//                                        data = "*" + BaseUtil.encodeStr(data) + "*";
                                    } else if (data.startsWith(",") && data.endsWith(",")) {
                                        data = StringUtils.strip(data, ",");
                                        data = data.replaceAll("，", ",");
                                        String[] arrData = data.split(",");
                                        String newData = "";
                                        int i = 0;
                                        for (String enc : arrData) {
                                            if (i < arrData.length - 1) {
                                                newData += BaseUtil.encodeStr(enc) + ",";
                                            } else {
                                                newData += BaseUtil.encodeStr(enc);
                                            }
                                            i++;
                                        }
//                                        data = "," + data + "," + newData+",";
                                        encodeData = newData;
                                    } else {
                                        encodeData = BaseUtil.encodeStr(data);
//                                        data = BaseUtil.encodeStr(data);
                                    }
                                    // 所有转码后的字段，需要以原值or转码后的值查询
                                    data = "," + data + "," + encodeData + ",";
                                    jsonObject.put(key, data);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    //判断是否是数字
    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    //判断字符串是否包含连续数字
    public static boolean isContinuityInteger(String str) {
        String reg="^.*\\d{6}.*$";
        if(str.matches(reg)){
            return true;
        }
        return false;
    }

    //截取字符串中连续的数字
    public static String getNumbers(String content) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    /**
     * 短信验证码
     * @param number
     * @return
     */
    public static String getRandomNumber(int number) {
        String codeNum = "";
        int [] numbers = {0,1,2,3,4,5,6,7,8,9};
        Random random = new Random();
        for (int i = 0; i < number; i++) {
            int next = random.nextInt(10000);//目的是产生足够随机的数，避免产生的数字重复率高的问题
            codeNum+=numbers[next%10];
        }
        return codeNum;
    }

    public static void main(String[] args) {
        System.out.println(isContinuityInteger("275357 認証コード： "));
        System.out.println(getNumbers("認証コード： 275357"));
        System.out.println(getNumbers("559822 is your verification code"));
    }
}
