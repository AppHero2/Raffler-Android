package com.raffler.app.utils;

/**
 * Created by Ghost on 2/9/2017.
 */

public class TimeUtil {

    public final static long ONE_MILLISECOND = 1;
    public final static long MILLISECONDS_IN_A_SECOND = 1000;

    public final static long ONE_SECOND = 1000;
    public final static long SECONDS_IN_A_MINUTE = 60;

    public final static long ONE_MINUTE = ONE_SECOND * 60;
    public final static long MINUTES_IN_AN_HOUR = 60;

    public final static long ONE_HOUR = ONE_MINUTE * 60;
    public final static long HOURS_IN_A_DAY = 24;
    public final static long ONE_DAY = ONE_HOUR * 24;
    public final static long DAYS_IN_A_YEAR = 365;

    public static String formatHMSM(Number n) {

        String res = "";
        if (n != null) {
            long duration = n.longValue();

            duration /= ONE_MILLISECOND;
            int milliseconds = (int) (duration % MILLISECONDS_IN_A_SECOND);
            duration /= ONE_SECOND;
            int seconds = (int) (duration % SECONDS_IN_A_MINUTE);
            duration /= SECONDS_IN_A_MINUTE;
            int minutes = (int) (duration % MINUTES_IN_AN_HOUR);
            duration /= MINUTES_IN_AN_HOUR;
            int hours = (int) (duration % HOURS_IN_A_DAY);
            duration /= HOURS_IN_A_DAY;
            int days = (int) (duration % DAYS_IN_A_YEAR);
            duration /= DAYS_IN_A_YEAR;
            int years = (int) (duration);
            if (days == 0) {
                res = String.format("%02d:%02d:%02d", hours, minutes, seconds);//String.format("%02dh%02dm%02ds", hours, minutes, seconds);
            } else if (years == 0) {
                res = String.format("%ddays %02d:%02d:%02d", days, hours, minutes, seconds);//String.format("%ddays %02dh%02dm%02ds", days, hours, minutes, seconds);
            } else {
                res = String.format("%dyrs %ddays %02d:%02d:%02d", years, days, hours, minutes, seconds);//String.format("%dyrs %ddays %02dh%02dm%02ds", years, days, hours, minutes, seconds);
            }
        }
        return res;

    }
}
