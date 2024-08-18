package net.my.util;

import java.util.Base64;

public class Base64Util {
    public static String encode(String b64) {
        return Base64.getEncoder().encodeToString(b64.getBytes());
    }

    public static String decode(String b64) {
        return new String(Base64.getDecoder().decode(b64));
    }

    // URL安全编码（使用Base64URL格式）
    public static String urlEncode(String b64) {
        return Base64.getUrlEncoder().encodeToString(b64.getBytes()).replaceAll("=", "");
    }

    // URL安全解码（使用Base64URL格式）
    public static String urlDecode(String b64) {
        return new String(Base64.getUrlDecoder().decode(b64));
    }

    private static String formatStrR(String str) {
        return String.format("%-20s", str);
    }

    private static String formatStrL(String str) {
        return String.format("%20s", str);
    }
}
