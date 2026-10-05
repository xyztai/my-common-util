package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgMACDMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RawPO;
import net.my.util.MACDCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    private AgMACDMapper mapper;

    /**
     * demo:1.603920
     * @param code
     * @return
     */
    @GetMapping("/history-all/stock")
    public BaseResponse historyAllStock(@RequestParam("code") String code) {
        log.info("historyAllStock start...");

        List<RawPO> calcPrices = mapper.getClosePrices(code);
        if(CollectionUtils.isEmpty(calcPrices)) {
            log.info("calcPrices is empty");
            return BaseResponse.OK;
        }

        List<MACDCalculator.MACDResult> macdResults = calculate(calcPrices.stream().map(RawPO::getClosePrice).collect(Collectors.toList()));

        if(!CollectionUtils.isEmpty(macdResults)) {
            for (int i = 0; i < macdResults.size(); i++) {
                MACDCalculator.MACDResult r = macdResults.get(i);
                log.info("historyAllStock: {}", String.format("Day %d, tradeDate=%s: DIF=%.4f, DEA=%.4f, MACD=%.4f", i + 1, calcPrices.get(i).getTradeDate(), r.dif, r.dea, r.macd));
            }
        }

        log.info("historyAllStock end...");
        return BaseResponse.OK;
    }

//    @GetMapping("/history-all/stock")
//    public BaseResponse historyAllStock() {
//        log.info("historyAllStock start...");
//
//        List<String> calcDates = mapper.getCalcMACDDates4Stock();
//        if(!CollectionUtils.isEmpty(calcDates)) {
//            for(String calcDate : calcDates) {
//                log.info("calcDate: {}", calcDate);
//                mapper.genMACDData4Stock(calcDate);
//            }
//        }
//
//        log.info("historyAllStock end...");
//        return BaseResponse.OK;
//    }

//    @GetMapping("/history-all/etf")
//    public BaseResponse historyAllEtf() {
//        log.info("historyAllEtf start...");
//
//        List<String> calcDates = mapper.getCalcMACDDates4Etf();
//        if(!CollectionUtils.isEmpty(calcDates)) {
//            for(String calcDate : calcDates) {
//                log.info("calcDate: {}", calcDate);
//                mapper.genMACDData4Etf(calcDate);
//            }
//        }
//
//        log.info("historyAllEtf end...");
//        return BaseResponse.OK;
//    }
}

