package com.app.common.util;

import java.util.Calendar;
import java.util.TimeZone;

public class UsDateUtil {

    public static long dayStart(int afterDays) {
        Calendar cal = getUsCalendar();
        cal.add(Calendar.DAY_OF_MONTH, afterDays);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static Calendar getUsCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT-8"));
    }
}
