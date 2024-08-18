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

    public static void main(String[] args) {
        String b64 = "eyJwYWdlVXJsIjoiaHR0cHM6Ly90cGhoLmxpZmUuY250YWlwaW5nLmNvbS93ZWItdHBoaC9tZXJjaGFudC1oNS8_YXBwSWQ9MTA2JmNvZGU9JHt3aW5rVG9rZW59Iy8iLCAicGxhbnRJZCI6IjEwNSIsICJkc3RQbGFudElkIjoiMTA1In0";
        System.out.println(formatStrL("b64=") + b64);
        String str = urlDecode(b64);
        System.out.println(formatStrL("str=") + str);
        String urlEncodeStr = urlEncode(str);
        System.out.println(formatStrL("urlEncodeStr=") + urlEncodeStr);
        String encodeStr = encode(str);
        System.out.println(formatStrL("encodeStr=") + encodeStr);
        String decodeStr = decode(encodeStr);
        System.out.println(formatStrL("decodeStr=") + decodeStr);
    }
}
