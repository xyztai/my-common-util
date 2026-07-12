package net.my.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.AgEastmoneyStockStrategyMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import net.my.pojo.SpecialCarePoJo2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-eastmoney-stock-strategy")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgEastmoneyStockStrategyController {

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private AgEastmoneyStockStrategyMapper mapper;

    /* 策略1
     T日（比如2026-06-11）出现买点机会，看T+1日（比如2026-06-12）
     如果T+1的收盘chg<-5%,则在T+2,以(T+1)的收盘价格*（1-4%）的价格buy
     如果T+1的收盘chg>-5%,则在尾盘以close价格buy
     持有最多3天
     */
    private static final String KEY_88801 = "stock#" + "strategy_1";

    /* 策略2
     T日（比如2026-06-11）出现买点机会，看T+1日（比如2026-06-12）
     如果T+1的收盘chg<-5%,则在T+2,以(T+1)的收盘价格*（1-4%）的价格buy
     如果T+1的收盘chg>-5%,则在尾盘以close价格buy
     持有最多3天
     */
    private static final String KEY_88802 = "stock#" + "strategy_2";

    /* 策略3
     查询近几日的热门板块的top10的情况，会标记是否多头
     */
    private static final String KEY_88803 = "stock#" + "strategy_3";


    /**
     * 88801、
     * * @return
     */
    @GetMapping("/strategy_1")
    public BaseResponse strategy_1() {
        log.info("strategy_1");
        String key = KEY_88801;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = mapper.strategy_1();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }


    /**
     * 88802、
     * * @return
     */
    @GetMapping("/strategy_2")
    public BaseResponse strategy_2() {
        log.info("strategy_2");
        String key = KEY_88802;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = mapper.strategy_2();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }


    /**
     * 88803、
     * * @return
     */
    @GetMapping("/strategy_3")
    public BaseResponse strategy_3() {
        log.info("strategy_3");
        String key = KEY_88803;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = mapper.strategy_3();
//        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
//                .filter(f -> !f.getStockCode().startsWith("688")
//                        && !f.getStockCode().startsWith("689")
//                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

}

