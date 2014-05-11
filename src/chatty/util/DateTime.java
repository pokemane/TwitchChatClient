
package chatty.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Stuff to do with dates.
 * 
 * @author tduva
 */
public class DateTime {
    
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm");
    private static final int MINUTE = 60;
    private static final int HOUR = MINUTE * 60;
    private static final int DAY = HOUR * 24;
    
    public static String currentTime(SimpleDateFormat sdf) {
        Calendar cal = Calendar.getInstance();
    	
        return sdf.format(cal.getTime());
    }
    
    public static String currentTime() {
        return currentTime(SDF);
    }
    
    public static String format(long time, SimpleDateFormat sdf) {
        return sdf.format(new Date(time));
    }
    
    public static String format(long time) {
        return SDF.format(new Date(time));
    }
    
    public static String format2(long time) {
        return SDF2.format(new Date(time));
    }
    
    public static String ago(long time) {
        long timePassed = System.currentTimeMillis() - time;
        return ago2(timePassed);
        
    }
    
    public static String ago2(long timePassed) {
        long seconds = timePassed / 1000;
        if (seconds < MINUTE*10) {
            return "just now";
        }
        if (seconds < HOUR) {
            return "recently";
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+" "+(hours == 1 ? "hour" : "hours")+" ago";
        }
        int days = (int)seconds / DAY;
        return days+" "+(days == 1 ? "day" : "days")+" ago";
    }
    
    public static String duration(long time, boolean detailed) {
        return duration(time, detailed, true);
    }
    
    public static String duration(long time, boolean detailed, boolean milliseconds) {
        long seconds = time;
        if (milliseconds) {
            seconds = time / 1000;
        }
        if (seconds < MINUTE) {
            return seconds+"s";
        }
        if (seconds < HOUR) {
            int s = (int)seconds % MINUTE;
            if (detailed && s > 0) {
                return seconds / MINUTE+"m "+s+"s";
            }
            return seconds / MINUTE+"m";
        }
        if (seconds < DAY) {
            return seconds / HOUR+"h";
        }
        return seconds / DAY+"d";
    }
}
