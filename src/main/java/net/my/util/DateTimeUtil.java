package net.my.util;

import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtil {
    public static void main(String[] args) {
        try {
            System.out.println("xxx " + getDateStrFromTimeStamp(1769097600000L));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            long timestamp = 1769097600000L;
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
            // 指定时区（例如：亚洲上海）
            String zoneIdStr = "Asia/Shanghai";

            // 将时间戳转换为Instant对象
            Instant instant = Instant.ofEpochMilli(timestamp);

            // 创建指定时区的ZonedDateTime对象
            ZoneId zoneId = ZoneId.of(zoneIdStr);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);

            // 格式化输出
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            String formattedDate = zonedDateTime.format(formatter);

            if(!StringUtils.isEmpty(formattedDate)) {
                return formattedDate.substring(0, 10);
            }

            return "";
        } catch (Exception ex) {
            return "";
        }
    }
}
