package net.my.util;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Base64UtilTest {

    @Test
    public void test001_urlEncode() {
        String str = "{\"a\":\"1\", \"b\": \"2\"}";
        System.out.println(Base64Util.urlEncode(str));
    }

    @Test
    public void test002_urlDecode() {
        String b64 = "eyJhIjoiMSIsICJiIjogIjIifQ";
        System.out.println(Base64Util.urlDecode(b64));
    }
}