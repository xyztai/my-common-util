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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-sohu")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewSohuController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AgSohuMapper agSohuMapper;

    @GetMapping("/history")
    @Transactional
    public BaseResponse test() {
        return RestGeneralResponse.of(getQQResReplaceEastmoney()); // 只读取最新一天的
    }

    // demo
    // https://hqm.stock.sohu.com/getqjson?code=cn_600875,cn_688082
    public static final String SO_HU_URL_FORMAT =
            "https://hqm.stock.sohu.com/getqjson?code=%s";

    public Map<String, String> getQQResReplaceEastmoney() {
        List<String> hsStocks = agSohuMapper.getStocks();
        List<String> hsEtfs = agSohuMapper.getEtfs();
        List<String> allCodes = new ArrayList<>();
        if(!CollectionUtils.isEmpty(hsStocks)) {
            allCodes.addAll(hsStocks);
        }
        if(!CollectionUtils.isEmpty(hsEtfs)) {
            allCodes.addAll(hsEtfs);
        }

        Map<String, String> resMap = new HashMap<>();
        if(CollectionUtils.isEmpty(allCodes)) {
            return resMap;
        }

        Map<String, String> nameMap = new HashMap<>();
        allCodes.forEach(f -> {
            nameMap.put("cn_" + f.substring(2), f);
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
                String url = String.format(SO_HU_URL_FORMAT, sohuCode);
                log.info("url: {}", url);

                // 创建请求头
                HttpHeaders headers = new HttpHeaders();
                headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36");

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
                Map<String, Object> soHuRes = JSON.parseObject(res, Map.class);
                log.info("soHuRes={}", JSON.toJSON(soHuRes));
                for(Map.Entry<String, Object> entry : soHuRes.entrySet()) {
                    String key = entry.getKey();
                    Object obj = entry.getValue();
                    List<String> values = JSON.parseArray(obj.toString(), String.class);
                    List<String> targetValues = new ArrayList<>();
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
}
