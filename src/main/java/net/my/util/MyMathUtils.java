package net.my.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MyMathUtils {
    public static Double getRoundDouble(Double dou) {
        BigDecimal bd = new BigDecimal(Double.toString(dou));
        bd = bd.setScale(3, RoundingMode.HALF_UP); // 保留3位小数，HALF_UP为四舍五入
        double roundedNumber = bd.doubleValue();
        return roundedNumber;
    }
}
