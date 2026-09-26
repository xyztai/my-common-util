package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgCCIEastmoneyStockMapper;
import net.my.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/ag-cci")
@Slf4j
public class AgNewEastmoneyCCIController {

    @Autowired
    private AgCCIEastmoneyStockMapper mapper;

    @GetMapping("/history-all/stock")
    public BaseResponse historyAllStock() {
        log.info("historyAllStock start...");

        List<String> calcDates = mapper.getCalcCCIDates4Stock();
        if(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("calcDate: {}", calcDate);
                mapper.genCCIData4Stock(calcDate);
            }
        }

        log.info("historyAllStock end...");
        return BaseResponse.OK;
    }

    @GetMapping("/history-all/etf")
    public BaseResponse historyAllEtf() {
        log.info("historyAllEtf start...");

        List<String> calcDates = mapper.getCalcCCIDates4Etf();
        if(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("calcDate: {}", calcDate);
                mapper.genCCIData4Etf(calcDate);
            }
        }

        log.info("historyAllEtf end...");
        return BaseResponse.OK;
    }
}

