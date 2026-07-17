package com.app.common.util;


import com.app.common.SpringContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis工具类
 */
public class RedisUtil {

    private final static StringRedisTemplate stringRedisTemplate = SpringContextHolder.getBean("stringRedisTemplate");

    /**
     * Redis JSON 序列化/反序列化使用的 ObjectMapper：
     * - 支持 java.time.*（LocalDateTime 等）
     * - 以字符串形式读写日期时间，避免类型不兼容问题
     */
    private final static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // 注册 Java8 时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 使用可读的时间字符串，而不是时间戳数组
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 匹配key
     */
    public static Set<String> keys(String pattern) {
        return stringRedisTemplate.keys(pattern);
    }

    /**
     * 删除一个key
     */
    public static void del(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 批量删除key
     */
    public static void delByPattern(String pattern) {
        Set<String> keySet = keys(pattern);
        stringRedisTemplate.delete(keySet);
    }

    /**
     * 设置过期时间，单位为秒
     */
    public static boolean expire(String key, long seconds) {
        return stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取自动过期时间
     */
    public static long ttl(String key) {
        return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 移除过期时间
     */
    public static boolean persist(String key) {
        return stringRedisTemplate.persist(key);
    }

    /**
     * 判断key是否存在
     */
    public static boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /////// String 操作

    /**
     * 给key赋值
     */
    public static void set(String key, String value) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        op.set(key, value);
    }

    /**
     * 给key赋值，并设置过期时间，单位为秒
     */
    public static void setEx(String key, String value, long seconds) {
        set(key, value);
        expire(key, seconds);
    }

    public static boolean setNxEx(String key, String value, long seconds) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        return op.setIfAbsent(key, value, Duration.ofSeconds(seconds));
    }

    public static boolean setNx(String key, String value) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        return op.setIfAbsent(key, value);
    }

    public static String getLockKey(String key) {
        return "DcLock:" + key + ":" + Md5Util.md5(key);
    }

    public static boolean tryLock(String key, long expireSecond) {
        String md5Key = getLockKey(key);
        return setNxEx(md5Key, "0", expireSecond);
    }

    public static void lock(String key) {
        lock(key, 2L);
    }

    public static void lock(String key, long expireSecond) {
        if (expireSecond > 60) {
            expireSecond = 60;
        }
        String md5Key = getLockKey(key);
        String value = "0";
        while (!setNxEx(md5Key, value, expireSecond)) {
            sleep(20);
        }
    }

    public static void unLock(String key) {
        String md5Key = getLockKey(key);
        del(md5Key);
    }

    public static void sleep(int timeMills) {
        try {
            Thread.sleep(timeMills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将key的值加num
     */
    public static void incrBy(String key, long num) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        op.increment(key, num);
    }


    /**
     * 将key的值加num
     */
    public static Double incrBy(String key, double num) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        return op.increment(key, num);
    }

    /**
     * 将key的值加num
     */
    public static Double incrBy(String key, double num, long seconds) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        Double d = op.increment(key, num);
        expire(key, seconds);
        return d;
    }

    /**
     * 获取key的值
     */
    public static String get(String key) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        return op.get(key);
    }

    /**
     * 读取对象（反序列化 JSON）
     */
    public static <T> T getObject(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入对象并设置过期时间（秒）
     */
    public static void setObject(String key, Object value, long seconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            setEx(key, json, seconds);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入对象（序列化为 JSON）
     */
    public static void setObject(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取key的值
     */
    public static Integer getNumber(String key) {
        ValueOperations<String, String> op = stringRedisTemplate.opsForValue();
        String value = op.get(key);
        return !StringUtils.isBlank(value) ? Integer.parseInt(value) : 0;
    }

    /////// list操作

    /**
     * 插入到表头
     */
    public static void lPush(String key, String... values) {
        ListOperations<String, String> listOp = stringRedisTemplate.opsForList();
        listOp.leftPushAll(key, values);
    }

    /**
     * 移除第一个
     */
    public static String rPop(String key) {
        ListOperations<String, String> listOp = stringRedisTemplate.opsForList();
        return listOp.rightPop(key);
    }

    /**
     * 移除第一个
     */
    public static String rPop(String key, int seconds) {
        ListOperations<String, String> listOp = stringRedisTemplate.opsForList();
        return listOp.rightPop(key, seconds, TimeUnit.SECONDS);
    }

    public static int lLen(String key) {
        ListOperations<String, String> opsForList = stringRedisTemplate.opsForList();
        return opsForList.size(key).intValue();
    }

    /**
     * 获取list所有
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public static List<String> lRange(String key, int start, int end) {
        ListOperations<String, String> opsForList = stringRedisTemplate.opsForList();
        return opsForList.range(key, start, end);
    }

    /////// hash

    /*
     * public static void hset(String key,String hashKey,String value){
     * HashOperations<String,String,String> opsForHash =
     * stringRedisTemplate.opsForHash(); opsForHash.put(key, hashKey, value); }
     */

    /**
     * 存放list
     *
     * @param key
     * @param list
     */
    public static void setList(String key, List<String> list) {
        ListOperations<String, String> opsForList = stringRedisTemplate.opsForList();
        opsForList.leftPushAll(key, list);
    }

    /**
     * 存放list
     *
     * @param key
     * @param list
     */
    public static void saveList(String key, String value) {
        ListOperations<String, String> opsForList = stringRedisTemplate.opsForList();
        opsForList.rightPush(key, value);
    }

    /**
     * 存放list
     *
     * @param key
     * @param list
     */
    public static List<String> getList(String key) {
        ListOperations<String, String> opsForList = stringRedisTemplate.opsForList();
        return opsForList.range(key, 0, -1);
    }

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public static String hGet(String key, String item) {
        HashOperations<String, String, String> mapOp = stringRedisTemplate.opsForHash();
        return mapOp.get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public static Map<String, String> hmGet(String key) {
        HashOperations<String, String, String> mapOp = stringRedisTemplate.opsForHash();
        return mapOp.entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public static boolean hmSet(String key, Map<String, String> map) {
        try {
            HashOperations<String, String, String> mapOp = stringRedisTemplate.opsForHash();
            mapOp.putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public static boolean hmset(String key, Map<String, String> map, long time) {
        try {
            HashOperations<String, String, String> mapOp = stringRedisTemplate.opsForHash();
            mapOp.putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public static boolean hset(String key, String item, String value) {
        try {
            HashOperations<String, String, String> mapOp = stringRedisTemplate.opsForHash();
            mapOp.put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public static boolean hset(String key, String item, Object value, long time) {
        try {
            HashOperations<String, Object, Object> mapOp = stringRedisTemplate.opsForHash();
            mapOp.put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public static void hdel(String key, Object... item) {
        HashOperations<String, Object, Object> mapOp = stringRedisTemplate.opsForHash();

        mapOp.delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public static boolean hHasKey(String key, String item) {
        HashOperations<String, Object, Object> mapOp = stringRedisTemplate.opsForHash();

        return mapOp.hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public static double hincr(String key, String item, double by) {
        HashOperations<String, Object, Object> mapOp = stringRedisTemplate.opsForHash();

        return mapOp.increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public static double hdecr(String key, String item, double by) {
        HashOperations<String, Object, Object> mapOp = stringRedisTemplate.opsForHash();

        return mapOp.increment(key, item, -by);
    }

    /////// set
    /////// sorted set
    public static boolean zAdd(String key, String value, int score) {
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        return ops.add(key, value, score);
    }
}
