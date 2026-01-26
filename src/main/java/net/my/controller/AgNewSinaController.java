package net.my.controller;

import com.alibaba.fastjson2.JSON;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgSohuMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-sina")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewSinaController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AgSohuMapper agSohuMapper;

    @GetMapping("/history")
    @Transactional
    public BaseResponse test() {
        return RestGeneralResponse.of(getQQResReplaceEastmoney(0)); // 只读取最新一天的
    }

    // demo
    // https://w.sinajs.cn/list=sh600039,sz300442
    public static final String SINA_URL_FORMAT =
            "https://w.sinajs.cn/list=%s";

    // demo
    // A,fjjs,0.0400,0.0615,-0.1003,3.1110,264.4417,23575.6,23575.6,23575.6,
    // todo... 换手率=成交量/流通股本，流通股本为结果的第9个，即[8]，其实意义不大，因为成交量就能看出来了，所以也不用计算了
    // https://w.sinajs.cn/rn=7522129656&list=sz001337_i,sz002679_i
    public static final String SINA_I_URL_FORMAT =
            "https://w.sinajs.cn/rn=7522129656&list=%s";


    public Map<String, String> getQQResReplaceEastmoney(Integer type) {
        List<String> allCodes = new ArrayList<>();

        if(type == 0 || type == 1) {
            List<String> hsStocks = agSohuMapper.getStocks();
            if(!CollectionUtils.isEmpty(hsStocks)) {
                allCodes.addAll(hsStocks);
            }
        }

        if(type == 0 || type == 2) {
            List<String> hsEtfs = agSohuMapper.getEtfs();
            if(!CollectionUtils.isEmpty(hsEtfs)) {
                allCodes.addAll(hsEtfs);
            }
        }

        Map<String, String> resMap = new HashMap<>();
        if(CollectionUtils.isEmpty(allCodes)) {
            return resMap;
        }

        Map<String, String> nameMap = new HashMap<>();
        allCodes.forEach(f -> {
            nameMap.put((f.substring(0, 2).equals("0.") ? "sz" : "sh") + f.substring(2), f);
        });

        List<String> allCodesCn = nameMap.keySet().stream().sorted().collect(Collectors.toList());
        List<String> targetSohuCodes = new ArrayList<>();
        int startNum = 0;
        int stepNum = 100;
        while(startNum < allCodesCn.size()) {
            List<String> tmpNodes = allCodesCn.stream().skip(startNum).limit(stepNum)
                    .collect(Collectors.toList());
            log.info("tmpNodes.size={}", tmpNodes.size());
            targetSohuCodes.add(String.join(",", tmpNodes));
            startNum += stepNum;
        }

        log.info("targetSohuCodes size={}", targetSohuCodes.size());
        for(String sohuCode : targetSohuCodes) {
            log.info("sohuCode={}", sohuCode);

            try {
                Thread.sleep(2000);
                String url = String.format(SINA_URL_FORMAT, sohuCode);
                log.info("url: {}", url);

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
                List<String> lines = Arrays.asList(res.split("\\R"));
                for(String line : lines) {
                    String[] entry = line.split("=");
                    String key = entry[0].substring(11);
                    String value = entry[1].replace("\"", "").replace(";", "");
//                    30,1,3,4,5,8/100,9,round((4-5)/2*100,2),round(3/2*100 -100,2),3-2,0
                    String[] fields = value.split(",");
                    if(Double.parseDouble(fields[8]) == 0 || Double.parseDouble(fields[9]) == 0) {
                        continue;
                    }

                    resMap.put(nameMap.get(key),
                            fields[30] + "," + fields[1] + "," + fields[3] + "," + fields[4] + "," + fields[5]
                            + "," + new BigDecimal(Double.parseDouble(fields[8])/100).setScale(0, RoundingMode.HALF_UP)
                            + "," + fields[9]
                            + "," + new BigDecimal((Double.parseDouble(fields[4]) - Double.parseDouble(fields[5]))/Double.parseDouble(fields[2]) * 100).setScale(2, RoundingMode.HALF_UP)
                            + "," + new BigDecimal(Double.parseDouble(fields[3])/Double.parseDouble(fields[2]) * 100 - 100).setScale(2, RoundingMode.HALF_UP)
                            + "," + new BigDecimal(Double.parseDouble(fields[3]) - Double.parseDouble(fields[2])).setScale(3, RoundingMode.HALF_UP)
                            + "," + "0"
                    );
                }
                /*String res = response.getBody();
                Map<String, Object> soHuRes = JSON.parseObject(res, Map.class);
                log.info("soHuRes={}", JSON.toJSON(soHuRes));
                for(Map.Entry<String, Object> entry : soHuRes.entrySet()) {
                    String key = entry.getKey();
                    Object obj = entry.getValue();
                    List<String> values = JSON.parseArray(obj.toString(), String.class);
                    List<String> targetValues = new ArrayList<>();
                    if(
                       (long)Double.parseDouble(values.get(3).replace("+", "").replace("%", "")) * 10000 == 0
                       && (long)Double.parseDouble(values.get(5)) == 0
                       && (long)Double.parseDouble(values.get(7)) == 0
                    ) {
                        continue;
                    }

                    targetValues.add(values.get(17).substring(0, 10));
                    targetValues.add(values.get(14));
                    targetValues.add(values.get(2));
                    targetValues.add(values.get(10));
                    targetValues.add(values.get(11));
                    targetValues.add(String.format("%d", (long)Double.parseDouble(values.get(5))));
                    targetValues.add(String.format("%d", (long)Double.parseDouble(values.get(7))*10000));
                    targetValues.add("0");
                    targetValues.add(values.get(3).replace("+", "").replace("%", ""));
                    targetValues.add(values.get(4).replace("+", "").replace("%", ""));
                    targetValues.add(values.get(8).replace("+", "").replace("%", ""));

                    resMap.put(nameMap.get(key), String.join(",", targetValues));
                }
*/
            } catch (Exception ex) {
                log.error("{}", ex);
                return new HashMap<>();
            }
        }

        if(!CollectionUtils.isEmpty(resMap)) {
            log.info("start log resMap, size={}", resMap.size());
            resMap.entrySet().forEach(f -> {
                log.info("key={}, value={}", f.getKey(), f.getValue());
            });
        }

        return resMap;
    }

    public String getMaxDateFromStock() {
        return agSohuMapper.getMaxDateFromStock();
    }

    public String getMaxDateFromEtf() {
        return agSohuMapper.getMaxDateFromEtf();
    }
}
