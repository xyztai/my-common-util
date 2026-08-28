package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgSohuMapper;
import net.my.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-sina-history")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewSinaHistoryController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AgSohuMapper agSohuMapper;

    // demo
    // http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sh600039&scale=240&ma=5&datalen=2
    public static final String SINA_URL_FORMAT =
            "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=240&ma=5&datalen=%d";


    @GetMapping("/history/{days}")
    @Transactional
    public BaseResponse getHistoryData(@PathVariable("days") Integer days, List<DataSohu> list1) {
        log.info("开始获取历史数据 days={} start", days);
        List<String> allCodes = new ArrayList<>();

        List<String> hsStocks = agSohuMapper.getStocks();
        if(!CollectionUtils.isEmpty(hsStocks)) {
            allCodes.addAll(hsStocks);
        }

        List<String> hsEtfs = agSohuMapper.getEtfs();
        if(!CollectionUtils.isEmpty(hsEtfs)) {
            allCodes.addAll(hsEtfs);
        }

        List<String> hsIndexStocks = agSohuMapper.getIndexs();
        if(!CollectionUtils.isEmpty(hsIndexStocks)) {
            allCodes.addAll(hsIndexStocks);
        }

        Map<String, String> resMap = new HashMap<>();
        if(CollectionUtils.isEmpty(allCodes)) {
            return BaseResponse.OK;
        }

        Map<String, String> nameMap = new HashMap<>();
        allCodes.forEach(f -> {
            nameMap.put((f.substring(0, 2).equals("0.") ? "sz" : "sh") + f.substring(2), f);
        });

        List<String> allCodesCn = nameMap.keySet().stream().sorted().collect(Collectors.toList());
        // 开始去访问历史数据，用来补充数据使用
        for(String code : allCodesCn) {
            log.info("开始补历史数据 code={}", code);
            String url = String.format(SINA_URL_FORMAT, code, days);
            log.info("url: {}", url);

            try {
                // 创建请求头
                HttpHeaders headers = new HttpHeaders();
                headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36");
                headers.add("referer", "https://quotes.sina.cn");

                // 创建HttpEntity
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // 发送GET请求
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );
                log.info("响应状态: " + response.getStatusCode());
                log.info("响应体: " + response.getBody());
                String res = response.getBody();
                log.info("res={}", res);
                List<DataSohu> sohuList = JSON.parseArray(res, DataSohu.class);
                log.info("sohuList={}", JSON.toJSON(sohuList));
            } catch (Exception ex) {
                log.info("", ex);
            }
        }

        return BaseResponse.OK;
    }

    @Data
    public class DataSohu {
        private String day;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Double volume;
    }
}
