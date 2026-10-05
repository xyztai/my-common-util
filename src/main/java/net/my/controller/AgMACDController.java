package net.my.controller;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.AgMACDMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RawPO;
import net.my.pojo.RestGeneralResponse;
import net.my.util.MACDCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.my.util.MACDCalculator.calculate;

/**
 * 获取指数值-MACD
 */
@RestController
@RequestMapping("/ag-macd")
@Slf4j
public class AgMACDController {

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private AgMACDMapper mapper;

    private static final String KEY_STOCK = "stock#MACD";

    private static final String KEY_ETF = "etf#MACD";

    /**
     * demo:1.603920
     * @return
     */
    @GetMapping("/history-all/stock")
    public BaseResponse historyAllStock(@RequestParam("codes") List<String> codes) {
        log.info("historyAllStock start...");

        String key = KEY_STOCK;
        List<LinkedHashMap<String, String>> res = (List<LinkedHashMap<String, String>>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        if(CollectionUtils.isEmpty(codes)) {
            codes = mapper.getAllCodesStock();
        }
        if(CollectionUtils.isEmpty(codes)) {
            log.info("codes is empty");
            return BaseResponse.OK;
        }

        Map<String, String> resMap = new LinkedHashMap<>();

        int startNum = 0;
        int stepNum = 50;
        while(startNum < codes.size()) {
            List<String> batchCodes = codes.stream().skip(startNum).limit(stepNum)
                    .collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(batchCodes)) {
                log.info("start calc {}~{}/{}", startNum + 1, startNum + batchCodes.size(), codes.size());
                log.info("batchCodes={}", JSON.toJSON(batchCodes));
                List<RawPO> calcPricesBatch = mapper.getClosePricesBatchStock(batchCodes);
                if(!CollectionUtils.isEmpty(calcPricesBatch)) {
                    for(String code : batchCodes) {
                        List<RawPO> calcPrices = calcPricesBatch.stream().filter(f -> f.getCode().equals(code)).collect(Collectors.toList());
                        if(CollectionUtils.isEmpty(calcPrices)) {
                            log.info("calcPrices(code={}) is empty", code);
                            continue;
                        }

                        List<MACDCalculator.MACDResult> macdResults = calculate(calcPrices.stream().map(RawPO::getClosePrice).collect(Collectors.toList()));

                        if(!CollectionUtils.isEmpty(macdResults)) {
                            for (int j = 0; j < macdResults.size(); j++) {
                                MACDCalculator.MACDResult r = macdResults.get(j);
                                if(r.macd > -0.5 && r.macd < 0.5 && calcPrices.get(j).getTradeDate().compareTo("2025-01-01") > 0) {
                                    resMap.put(code.substring(2) + "-" + calcPrices.get(j).getTradeDate(), String.format("tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
                                    log.info("historyAllStock: {}", String.format("Day %d, tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", j + 1, calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
                                }
                            }
                        }
                    }
                }
            }
            startNum += stepNum;
        }

//        for(int i = 0; i < codes.size(); i++) {
//            String code = codes.get(i);
//            log.info("start calc {}/{}, code={}", i+1, codes.size(), code);
//            List<RawPO> calcPrices = mapper.getClosePrices(code);
//            if(CollectionUtils.isEmpty(calcPrices)) {
//                log.info("calcPrices is empty");
//                return BaseResponse.OK;
//            }
//
//            List<MACDCalculator.MACDResult> macdResults = calculate(calcPrices.stream().map(RawPO::getClosePrice).collect(Collectors.toList()));
//
//            if(!CollectionUtils.isEmpty(macdResults)) {
//                for (int j = 0; j < macdResults.size(); j++) {
//                    MACDCalculator.MACDResult r = macdResults.get(j);
//                    if(r.macd > -0.5 && r.macd < 0.5 && calcPrices.get(j).getTradeDate().compareTo("2025-01-01") > 0) {
//                        resMap.put(code + "-" + calcPrices.get(j).getTradeDate(), String.format("tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
//                        log.info("historyAllStock: {}", String.format("Day %d, tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", j + 1, calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
//                    }
//                }
//            }
//        }

//        if(!CollectionUtils.isEmpty(resMap)) {
//            resMap.entrySet().forEach(f -> log.info("key={}, value={}", f.getKey(), f.getValue()));
//        }

        log.info("historyAllStock end...");
        myCaffeineCache.put(key, resMap);
        log.info("myCaffeineCache put, key={}, res={}", key, JSON.toJSON(resMap));
        return RestGeneralResponse.of(resMap);
    }

    /**
     * demo:1.513130
     * @return
     */
    @GetMapping("/history-all/etf")
    public BaseResponse historyAllEtf(@RequestParam("codes") List<String> codes) {
        log.info("historyAllEtf start...");

        String key = KEY_ETF;
        List<LinkedHashMap<String, String>> res = (List<LinkedHashMap<String, String>>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        if(CollectionUtils.isEmpty(codes)) {
            codes = mapper.getAllCodesEtf();
        }
        if(CollectionUtils.isEmpty(codes)) {
            log.info("codes is empty");
            return BaseResponse.OK;
        }

        Map<String, String> resMap = new LinkedHashMap<>();

        int startNum = 0;
        int stepNum = 50;
        while(startNum < codes.size()) {
            List<String> batchCodes = codes.stream().skip(startNum).limit(stepNum)
                    .collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(batchCodes)) {
                log.info("start calc {}~{}/{}", startNum + 1, startNum + batchCodes.size(), codes.size());
                log.info("batchCodes={}", JSON.toJSON(batchCodes));
                List<RawPO> calcPricesBatch = mapper.getClosePricesBatchEtf(batchCodes);
                if(!CollectionUtils.isEmpty(calcPricesBatch)) {
                    for(String code : batchCodes) {
                        List<RawPO> calcPrices = calcPricesBatch.stream().filter(f -> f.getCode().equals(code)).collect(Collectors.toList());
                        if(CollectionUtils.isEmpty(calcPrices)) {
                            log.info("calcPrices(code={}) is empty", code);
                            continue;
                        }

                        List<MACDCalculator.MACDResult> macdResults = calculate(calcPrices.stream().map(RawPO::getClosePrice).collect(Collectors.toList()));

                        if(!CollectionUtils.isEmpty(macdResults)) {
                            for (int j = 0; j < macdResults.size(); j++) {
                                MACDCalculator.MACDResult r = macdResults.get(j);
                                if(r.macd > -0.5 && r.macd < 0.5 && calcPrices.get(j).getTradeDate().compareTo("2025-01-01") > 0) {
                                    resMap.put(code.substring(2) + "-" + calcPrices.get(j).getTradeDate(), String.format("tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
                                    log.info("historyAllStock: {}", String.format("Day %d, tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", j + 1, calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
                                }
                            }
                        }
                    }
                }
            }
            startNum += stepNum;
        }

//        for(int i = 0; i < codes.size(); i++) {
//            String code = codes.get(i);
//            log.info("start calc {}/{}, code={}", i+1, codes.size(), code);
//            List<RawPO> calcPrices = mapper.getClosePrices(code);
//            if(CollectionUtils.isEmpty(calcPrices)) {
//                log.info("calcPrices is empty");
//                return BaseResponse.OK;
//            }
//
//            List<MACDCalculator.MACDResult> macdResults = calculate(calcPrices.stream().map(RawPO::getClosePrice).collect(Collectors.toList()));
//
//            if(!CollectionUtils.isEmpty(macdResults)) {
//                for (int j = 0; j < macdResults.size(); j++) {
//                    MACDCalculator.MACDResult r = macdResults.get(j);
//                    if(r.macd > -0.5 && r.macd < 0.5 && calcPrices.get(j).getTradeDate().compareTo("2025-01-01") > 0) {
//                        resMap.put(code + "-" + calcPrices.get(j).getTradeDate(), String.format("tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
//                        log.info("historyAllStock: {}", String.format("Day %d, tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", j + 1, calcPrices.get(j).getTradeDate(), r.dif, r.dea, r.macd));
//                    }
//                }
//            }
//        }

//        if(!CollectionUtils.isEmpty(resMap)) {
//            resMap.entrySet().forEach(f -> log.info("key={}, value={}", f.getKey(), f.getValue()));
//        }

        log.info("historyAllStock end...");
        myCaffeineCache.put(key, resMap);
        log.info("myCaffeineCache put, key={}, res={}", key, JSON.toJSON(resMap));
        return RestGeneralResponse.of(resMap);
    }

}

