package net.my.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        Double cpD = Double.parseDouble(cp);

        Map<String, Double> resMapBuy = new LinkedHashMap<>();
        List<String> buyRatioList = Arrays.asList("0.975", "0.980", "0.985", "0.990");
        for(String ratio : buyRatioList) {
            resMapBuy.put(ratio, getRoundDouble(cpD * Double.parseDouble(ratio)));
        }

        Map<String, Double> resMapSell = new LinkedHashMap<>();
        List<String> sellRatioList = Arrays.asList("1.010", "1.015", "1.020", "1.025");
        for(String ratio : sellRatioList) {
            resMapSell.put(ratio, getRoundDouble(cpD * Double.parseDouble(ratio)));
        }


        Map<String, Object> resMap = new LinkedHashMap<>();
        resMap.put("buy", resMapBuy);
        resMap.put("**** cp", cpD + " **********");
        resMap.put("sell", resMapSell);
        return RestGeneralResponse.of(resMap);
    }

    private Double getRoundDouble(Double dou) {
        BigDecimal bd = new BigDecimal(Double.toString(dou));
        bd = bd.setScale(3, RoundingMode.HALF_UP); // 保留四位小数，HALF_UP为四舍五入
        double roundedNumber = bd.doubleValue();
        return roundedNumber;
    }
}
