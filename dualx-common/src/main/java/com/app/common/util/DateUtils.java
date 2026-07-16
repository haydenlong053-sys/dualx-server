package com.app.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 时间工具类
 * 
 * @author HayDen
 */
@Slf4j
public class DateUtils extends org.apache.commons.lang3.time.DateUtils
{
    public static String YYYY = "yyyy";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private static String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    public static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
    public static SimpleDateFormat format1 = new SimpleDateFormat(
            "yyyyMMdd HH:mm:ss");

    /**
     * 获取今天开始时间
     */
    public static String getStartTime() {
        Date date = new Date();
        Calendar dateStart = Calendar.getInstance();
        dateStart.setTime(date);
        dateStart.set(Calendar.HOUR_OF_DAY, 0);
        dateStart.set(Calendar.MINUTE, 0);
        dateStart.set(Calendar.SECOND, 0);
        return dateByString(dateStart.getTime());
    }

    /**
     * 字符串转Date
     * @param date1
     * @return
     */
    public static Date stringByDate(String date1){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);//注意月份是MM
        Date date = null;
        try {
            date = simpleDateFormat.parse(date1);
        } catch (ParseException e) {
            log.warn("日期解析失败, value={}", date1, e);
        }
        return date;
    }

    /**
     * 获取今天结束时间
     */
    public static String getEndTime() {
        Date date = new Date();
        Calendar dateEnd = Calendar.getInstance();
        dateEnd.setTime(date);
        dateEnd.set(Calendar.HOUR_OF_DAY, 23);
        dateEnd.set(Calendar.MINUTE, 59);
        dateEnd.set(Calendar.SECOND, 59);
        return dateByString(dateEnd.getTime());
    }

    /**
     * 在基础时间上加或者减一天
     *
     * @param date 当前时间
     * @param day  正数为加负数为减
     * @return
     */
    public static Date getNextDay(Date date, Integer day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, day);//+1今天的时间加一天
        date = calendar.getTime();
        return date;
    }

    /**
     * 得到指定日期的一天的的最后时刻23:59:59
     *
     * @param date
     * @return
     */
    public static Date getFinallyDate(Date date) {
        String temp = format.format(date);
        temp += " 23:59:59";

        try {
            return format1.parse(temp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 给指定时间加上秒
     * @param date
     * @return
     */
    public static Date timePastTenSecond(Date date,Integer ss) {
        try {
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Calendar newTime = Calendar.getInstance();
            newTime.setTime(date);
            newTime.add(Calendar.SECOND,ss);//日期加10秒

            Date dt1=newTime.getTime();
            return dt1;
        }
        catch(Exception ex) {
            log.warn("时间加秒失败, date={}, seconds={}", date, ss, ex);
            return null;
        }
    }

    /**
     * 得到指定日期的一天的开始时刻00:00:00
     *
     * @param date
     * @return
     */
    public static Date getStartDate(Date date) {
        String temp = format.format(date);
        temp += " 00:00:00";

        try {
            return format1.parse(temp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }


    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     * 
     * @return String
     */
    public static String getDate()
    {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static final String getTime()
    {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static final String dateTimeNow()
    {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static final String dateTimeNow(final String format)
    {
        return parseDateToStr(format, new Date());
    }

    public static final String dateTime(final Date date)
    {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    public static final String parseDateToStr(final String format, final Date date)
    {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 将日期格式化
     *
     * @param date
     * @return
     */
    public static String dateByString(Date date) {
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String tablename = dateFormat.format(date);
            return tablename;
        }
        return null;
    }

    public static String dateToHhMmSs(Date date) {
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String tablename = dateFormat.format(date);
            return tablename;
        }
        return null;
    }


    public static final Date dateTime(final String format, final String ts) {
        try {
            return new SimpleDateFormat(format).parse(ts);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static final String datePath()
    {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 日期路径 即年/月/日 如20180808
     */
    public static final String dateTime()
    {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyyMMdd");
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str)
    {
        if (str == null)
        {
            return null;
        }
        try
        {
            return parseDate(str.toString(), parsePatterns);
        }
        catch (ParseException e)
        {
            return null;
        }
    }
    
    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate()
    {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算相差天数
     */
    public static int differentDaysByMillisecond(Date date1, Date date2)
    {
        return Math.abs((int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24)));
    }

    /**
     * 计算两个时间差
     */
    public static String getDatePoor(Date endDate, Date nowDate)
    {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 字符串转换成日期
     *
     * @param str
     * @return date
     */
    public static Date StrToDate(String str) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            log.warn("日期时间解析失败, value={}", str, e);
        }
        return date;
    }

    /**
     * 字符串转换成日期
     *
     * @param str
     * @return date
     */
    public static Date StrToHhMmSs(String str) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            log.warn("时间解析失败, value={}", str, e);
        }
        return date;
    }

    /**
     * 判断当前时间是否在[startTime, endTime]区间，注意时间格式要一致
     *
     * @param nowTime   当前时间
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return
     * @author jqlin
     */
    public static boolean isEffectiveDate(Date nowTime, Date startTime, Date endTime) {
        if (nowTime.getTime() == startTime.getTime()
                || nowTime.getTime() == endTime.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断时间是否在时间段内
     *
     * @param nowTime
     * @param beginTime
     * @param endTime
     * @return
     */
    public static boolean belongCalendar(Date nowTime, Date beginTime,
                                         Date endTime) {
        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(beginTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 给指定的日期加上分钟后得到一个时间
     * @param date
     * @param minute     要加的分钟数
     * @return
     */
    public static Date addMinute(Date date,Integer minute) {
        Calendar c = Calendar.getInstance();
        c.setTime(date); //设置时间
        c.add(Calendar.MINUTE, minute); //日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
        date = c.getTime(); //结果
        return date;
    }

    /**
     * 根据两个时间判断当前时间是否在两个时间范围内
     * @param begin
     * @param end
     * @return
     */
    public Boolean ifDateInner(Date begin,Date end){
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");// 设置日期格式
        Date now = null;
        Date beginTime = null;
        Date endTime = null;
        try {
            now = df.parse(df.format(new Date()));
            beginTime = df.parse(dateToHhMmSs(begin));
            endTime = df.parse(dateToHhMmSs(end));
        } catch (Exception e) {
            log.warn("时间区间解析失败, begin={}, end={}", begin, end, e);
        }
        return belongCalendar(now, beginTime, endTime);
    }
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static String addDay(String date, int i) {
        try {
            GregorianCalendar gCal = new GregorianCalendar(
                    Integer.parseInt(date.substring(0, 4)),
                    Integer.parseInt(date.substring(5, 7)) - 1,
                    Integer.parseInt(date.substring(8, 10)));
            gCal.add(GregorianCalendar.DATE, i);
            return sdf.format(gCal.getTime());
        } catch (Exception e) {
            return getDate();
        }
    }
}
