package net.my.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ag-calc")
@Slf4j
@Api(value = "ag-calc", description = "ag计算接口")
public class AgCalcController {

    @GetMapping("/calc/{cp}")
    public BaseResponse getCalcData(@PathVariable("cp") String cp) {
        Map<Double, Double> resMapBuy = new LinkedHashMap<>();
        List<Double> buyRatioList = Arrays.asList(0.975, 0.98, 0.985, 0.99);
        Double cpD = Double.parseDouble(cp);
        for(Double ratio : buyRatioList) {
            resMapBuy.put(ratio, cpD * ratio);
        }

        Map<Double, Double> resMapSell = new LinkedHashMap<>();
        List<Double> sellRatioList = Arrays.asList(1.01, 1.015, 1.02, 1.025);
        for(Double ratio : sellRatioList) {
            resMapSell.put(ratio, cpD * ratio);
        }


        Map<String, Object> resMap = new LinkedHashMap<>();
        resMap.put("buy", resMapBuy);
        resMap.put("=====", "==========");
        resMap.put("sell", resMapSell);
        return RestGeneralResponse.of(resMap);
    }
}
