package net.my.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgWeekEastmoneyStockMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.EastmoneyNode;
import net.my.pojo.HsStockPoJo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-week-eastmoney-stock")
@Slf4j
public class AgNewEastmoneyWeekController {

    @Autowired
    private AgWeekEastmoneyStockMapper mapper;

    /**
     * 127.0.0.1:51666//ag-week-eastmoney-stock/test?cookieFromWeb=xxx&targetDate=2026-04-10
     * 通过浏览器访问 https://finance.eastmoney.com/ 来获取 cookie，然后作为参数给到参数，参数里面的 targetDate 为计算周的最后一个交易日的时间比如：2026-04-10
     * @param cookieFromWeb
     * @param targetDate
     * @return
     */
    @GetMapping("/test")
    public BaseResponse test(@RequestParam("cookieFromWeb") String cookieFromWeb, @RequestParam("targetDate") String targetDate) {
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


            List<HsStockPoJo> hs300List = mapper.getHs300List(targetDate);

            if(CollectionUtils.isEmpty(hs300List)) {
                return BaseResponse.OK;
            }

            List<String> eList = hs300List.stream().map(po -> 0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode())
                    .collect(Collectors.toList());


            List<EastmoneyNode> eastmoneyNodeList = new ArrayList<>();

            int i = 0;
            for(String e : eList) {
                i++;
                log.info("get e={}, seq:{}/{}", e, i, eList.size());
                String maxDate = mapper.getMaxEastMoneyNode(e);
                if(maxDate != null) {
                    if(maxDate.compareTo(targetDate) >= 0) {
                        continue;
                    }
                }

                // 5. 测试：用这个Cookie访问东财行情接口
                // beg=0 表示获取全量的历史数据
//                String url = String.format("https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=102&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
//                , e);

                // beg=20260301 表示只获取 20260301 以后的数据
                String url = String.format("https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=102&fqt=1&beg=20260301&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"
                        , e);

                log.info("url={}", url);
                try {
                    String res = testWithCookie(client, cookieFromWeb, url);

                    List<String> klines = new ArrayList<>();
                    if(!StringUtils.isEmpty(res)) {
                        log.info("res={}", res);
                        AgNewEastmoneyStockController.EastmoneyStockRes eastmoneyRes = JSON.parseObject(res, AgNewEastmoneyStockController.EastmoneyStockRes.class);
                        log.info("eastmoneyRes={}", JSON.toJSONString(eastmoneyRes));

                        if(eastmoneyRes == null || eastmoneyRes.getData() == null || CollectionUtils.isEmpty(eastmoneyRes.getData().getKlines())) {
                            continue;
                        }

                        klines = eastmoneyRes.getData().getKlines();
                    } else {
                        continue;
                    }

                    List<EastmoneyNode> nodes = new ArrayList<>();
                    for(String item : klines) {
                        String[] xxs = item.split(",");
                        nodes.add(EastmoneyNode.builder().date(xxs[0]).stockCode(e).infoRaw(item).build());
                    }

                    if(maxDate != null) {
                        nodes = nodes.stream()
                                .filter(f -> f.getDate().compareTo(maxDate) > 0).collect(Collectors.toList());
                    }

                    if(!CollectionUtils.isEmpty(nodes)) {
                        eastmoneyNodeList.addAll(nodes);
                    }

//                    log.info("get seq:{}/{}", i, eList.size());
                    if(eastmoneyNodeList.size() > 1000) {
                        int startNum = 0;
                        int stepNum = 500;
                        while(startNum < eastmoneyNodeList.size()) {
                            List<EastmoneyNode> tmpNodes = eastmoneyNodeList.stream().skip(startNum).limit(stepNum).collect(Collectors.toList());
                            log.info("tmpNodes.size={}", tmpNodes.size());
                            mapper.saveEastMoneyDatas(tmpNodes);
                            startNum += stepNum;
                        }
                        // 清空一下
                        eastmoneyNodeList = new ArrayList<>();
                    } else {
                        // 不处理数据就暂停2s
                        Thread.sleep(2000L);
                    }
                } catch (Exception ex) {
                    log.error("", ex);
                    break;
                }

//                if(i > 100) {
//                    break;
//                }
            }

            int startNum = 0;
            int stepNum = 500;
            while(startNum < eastmoneyNodeList.size()) {
                List<EastmoneyNode> tmpNodes = eastmoneyNodeList.stream().skip(startNum).limit(stepNum).collect(Collectors.toList());
                log.info("tmpNodes.size={}", tmpNodes.size());
                mapper.saveEastMoneyDatas(tmpNodes);
                startNum += stepNum;
            }

            log.info("阶段1-非99999数据-开始更新基础字段");
            // 更新基础字段
            mapper.updateEastMoneyDatas();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return BaseResponse.OK;
    }

    // 测试：带上Cookie请求东财数据
    String testWithCookie(OkHttpClient client, String cookie, String url) throws Exception {
//        cookie = "st_nvi=QhPv6Crs6CMGiOX_S5mYY54b7; st_si=24076036508251; nid18=082948fd4ab8ceb33932ece2c693ed1f; nid18_create_time=1775989707583; gviem=WfrzVJ5-PvCxRjzr_yP8g3fc5; gviem_create_time=1775989707583; fullscreengg=1; fullscreengg2=1; p_origin=https%3A%2F%2Fpassport2.eastmoney.com; qgqp_b_id=d04eb5857852683b567e1234e226779b; st_asi=delete; wsc_checkuser_ok=1; st_pvi=78291257023026; st_sp=2025-12-20%2020%3A32%3A04; st_inirUrl=https%3A%2F%2Fwww.baidu.com%2Flink; st_sn=43; st_psi=2026041220402642-113104312931-8454373463";

        Request testReq = new Request.Builder()
                .url(url) // 上证指数
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Cookie", cookie) // 带上Cookie
                .get()
                .build();

        try (Response resp = client.newCall(testReq).execute()) {
            String res = resp.body().string();
            System.out.println("\n行情接口响应码：" + resp.code());
            System.out.println("响应内容：" + res);
            return res;
        }
    }
}

