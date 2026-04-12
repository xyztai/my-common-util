package net.my.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgEastmoneyBkMapper;
import net.my.pojo.BaseResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/ag-week-eastmoney-stock")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyWeekController {

    public static void main(String[] args) {
        // 1. 构建OkHttp客户端（默认自动管理Cookie）
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        // 2. 伪装成Chrome浏览器（关键！）
        Request request = new Request.Builder()
                .url("https://www.eastmoney.com/") // 东财首页
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .addHeader("Connection", "keep-alive")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 3. 从响应头提取所有 Set-Cookie
            List<String> setCookies = response.headers("Set-Cookie");
            System.out.println("东方财富网返回Cookie数：" + setCookies.size());

            // 4. 拼接成完整Cookie字符串（后续请求直接用）
            StringBuilder cookieStr = new StringBuilder();
            for (String cookie : setCookies) {
                // 只取 key=value 部分（去掉;后面的Domain/Path/HttpOnly）
                String kv = cookie.split(";")[0].trim();
                if (cookieStr.length() > 0) {
                    cookieStr.append("; ");
                }
                cookieStr.append(kv);
            }

            System.out.println("\n完整Cookie（可直接复制使用）：");
            System.out.println(cookieStr);

            // 5. 测试：用这个Cookie访问东财行情接口
            testWithCookie(client, cookieStr.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 测试：带上Cookie请求东财数据
    private static void testWithCookie(OkHttpClient client, String cookie) throws Exception {
        Request testReq = new Request.Builder()
                .url("https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=0.002025&klt=102&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61") // 上证指数
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Cookie", cookie) // 带上Cookie
                .get()
                .build();

        try (Response resp = client.newCall(testReq).execute()) {
            System.out.println("\n行情接口响应码：" + resp.code());
            System.out.println("响应内容：" + resp.body().string());
        }
    }
}

