package net.my.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {
    public static void main(String[] args) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            long timestamp = 1769470679841L;
            Date date = new Date(timestamp);
            System.out.println(sdf.format(date));

            // 字符串转日期
            String dateString = "2026-01-01 00:00:00";
            date = sdf.parse(dateString);

            // 日期转时间戳
            timestamp = date.getTime();

            System.out.println("原始字符串: " + dateString);
            System.out.println("转换后的日期: " + date);
            System.out.println("对应的时间戳: " + timestamp);

            Date now = new Date();
            System.out.println(now.getTime() + 1000*3600*24*2);


        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static String getDateStrFromTimeStamp(Long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date(timestamp);
            return sdf.format(date);
        } catch (Exception ex) {
            return "";
        }
    }
}
