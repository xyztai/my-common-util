package net.my.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.AgEastmoneyEChartsMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.EchartsPoJo;
import net.my.pojo.RestGeneralResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-eastmoney-echarts")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyEChartsController {

    @Autowired
    private AgEastmoneyEChartsMapper agEastmoneyEChartsMapper;

    @Autowired
    private MyCaffeineCache myCaffeineCache;


    /**
     * 1、查询数据
     * @return
     */
    @GetMapping("/s69")
    public BaseResponse s69() {
        log.info("s69");
        String key = "echarts#" + "s69";
        List<EchartsPoJo> res = (List<EchartsPoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<EchartsPoJo> buyDataFromEastmoneys = agEastmoneyEChartsMapper.s69();
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            EchartsPoJo empty = new EchartsPoJo();
            empty.setName("--");
            empty.setValue1(0);
            empty.setValue2(0);
            empty.setValue3(0);
            empty.setValue4(0);
            empty.setValue5(0);
            empty.setValue6(0);
            empty.setValue7(0);
            empty.setValue8(0);
            empty.setValue9(0);
            empty.setValue10(0);
            empty.setValue11(0);
            empty.setValue12(0);
            buyDataFromEastmoneys = Arrays.asList(empty);
        }
        buyDataFromEastmoneys = buyDataFromEastmoneys
                .stream()
                .sorted(Comparator.comparing(EchartsPoJo::getName))
                .collect(Collectors.toList());

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }
}

