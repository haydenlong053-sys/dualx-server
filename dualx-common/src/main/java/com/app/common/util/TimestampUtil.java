package com.app.common.util;

import java.util.Calendar;
import java.util.TimeZone;

public class TimestampUtil {

    //far业务时间，北京时间，0点-0点
    public static long guanyunDayStart(int afterDays) {
        //return dayStart(afterDays, "GMT+8") + 12 * 3600_000;
        return projDayStart(afterDays);
    }

    public static long devDayStart(int afterDays){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        cal.add(Calendar.MINUTE, (cal.get(Calendar.MINUTE) % 10) * -1);
        cal.add(Calendar.MINUTE, afterDays * 10);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long todayTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long projDayStart(int afterDays) {
        //return dayStart(afterDays, "GMT+8") + 12 * 3600_000;
        return dayStart(afterDays, "GMT+8");
    }

    public static long utcDayStart(int afterDays) {
        return dayStart(afterDays, "UTC");
    }

    public static long dayStart(int afterDays, String zone) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        //cal.add(Calendar.HOUR_OF_DAY, -7);

        cal.add(Calendar.DAY_OF_MONTH, afterDays);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
