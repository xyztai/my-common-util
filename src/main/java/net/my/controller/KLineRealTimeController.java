package net.my.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.KLineRealTimeMapper;
import net.my.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/k-line-real-time")
@Slf4j
public class KLineRealTimeController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KLineRealTimeMapper kLineRealTimeMapper;

    // demo
    // https://web.sqt.gtimg.cn/q=sh600875,sh688082
    public static final String QQ_URL_FORMAT = "https://web.sqt.gtimg.cn/q=%s";


    @GetMapping("/qq")
    @Transactional
    public BaseResponse getHistoryData() {
        log.info("获取实时数据 start");
        List<String> allQQStocks = kLineRealTimeMapper.getAllQQStocks();
        if(CollectionUtils.isEmpty(allQQStocks)) {
            return BaseResponse.OK;
        }

        List<KLineRealTime> usefulLines = new ArrayList<>();
        try {
            String qqPara = String.join(",", allQQStocks);
            String url = String.format(QQ_URL_FORMAT, qqPara);
            log.info("url: {}", url);

            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36");
            headers.add("referer", "https://gu.qq.com/");

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
            List<String> lines = Arrays.asList(res.split("\\R"));
            if(CollectionUtils.isEmpty(lines)) {
                return BaseResponse.OK;
            }

            for(String line : lines) {
                Optional<String> code = allQQStocks.stream().filter(f -> line.contains(f)).findAny();
                if(code.isPresent()) {
                    KLineRealTime tmp = new KLineRealTime();
                    tmp.setSource("qq");
                    tmp.setKlineStr(line);
                    tmp.setCode(code.get());
                    String[] fields = line.split("~");
                    tmp.setKLineTime(fields[30]);
                    usefulLines.add(tmp);
                }
            }

            if(CollectionUtils.isEmpty(usefulLines)) {
                return BaseResponse.OK;
            }

            int startNum = 0;
            int stepNum = 200;
            while(startNum < usefulLines.size()) {
                List<KLineRealTime> tmpDatas = usefulLines.stream().skip(startNum).limit(stepNum)
                        .collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(tmpDatas)) {
                    log.info("tmpDatas.size={}", tmpDatas.size());
                    kLineRealTimeMapper.saveDataQQ(tmpDatas);
                }
                startNum += stepNum;
            }

        } catch (Exception ex) {
            log.info("", ex);
        }

        log.info("获取实时数据 end");
        return BaseResponse.OK;
    }

    @Data
    public class KLineRealTime {
        private String source;
        private String kLineTime;
        private String code;
        private String klineStr;
    }
}
