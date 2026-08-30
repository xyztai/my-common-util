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
import org.springframework.util.StringUtils;
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

    // https://qt.gtimg.cn/?q=s_sz002025 这里只返回简略信息
    // demo
    // https://web.sqt.gtimg.cn/q=sh600875,sh688082
    public static final String QQ_URL_FORMAT = "https://web.sqt.gtimg.cn/q=%s";


    @GetMapping("/qq")
    @Transactional
    public BaseResponse getDataFromQQ() {
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
                    try {
                        KLineRealTime tmp = new KLineRealTime();
                        tmp.setSource("qq");
                        tmp.setKlineStr(line);
                        tmp.setCode(code.get());
                        String[] fields = line.split("~");
                        if(StringUtils.isEmpty(fields[30]) || StringUtils.isEmpty(fields[1])) {
                            continue;
                        }

                        tmp.setKLineTime(fields[30]);

                        tmp.setDay(fields[30].substring(0, 8));
                        tmp.setStockName(fields[1]);

                        try {
                            tmp.setOpen(Double.parseDouble(fields[5]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setLast(Double.parseDouble(fields[3]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setHigh(Double.parseDouble(fields[33]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setLow(Double.parseDouble(fields[34]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setVolume(Double.parseDouble(fields[6]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setChg(Double.parseDouble(fields[32]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setZhengFu(Double.parseDouble(fields[43]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setAmount(Double.parseDouble(fields[37]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setExchangeRaw(Double.parseDouble(fields[38]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setTtm(Double.parseDouble(fields[39]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setPe(Double.parseDouble(fields[53]));
                        } catch (Exception ex) {
                            ;
                        }

                        try {
                            tmp.setVr(Double.parseDouble(fields[49]));
                        } catch (Exception ex) {
                            ;
                        }

                        usefulLines.add(tmp);
                    } catch (Exception ex) {
                        log.info("解析字符串失败", ex);
                    }
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

        private String day;
        private String stockName;
        private Double open;
        private Double last;
        private Double high;
        private Double low;
        private Double volume;
        private Double chg;
        private Double zhengFu;
        private Double amount;
        private Double exchangeRaw;

        private Double ttm; // 动态市盈率
        private Double pe; // 市盈率
        private Double vr; // 市盈率
    }
}
