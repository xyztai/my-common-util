package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.AgBOLLMapper;
import net.my.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 获取指数值-BOLL
 */
@RestController
@RequestMapping("/ag-boll")
@Slf4j
public class AgBOLLController {

    @Autowired
    private AgBOLLMapper mapper;

    @GetMapping("/history-all/stock")
    public BaseResponse historyAllStock() {
        log.info("historyAllStock start...");

        List<String> calcDates = mapper.getCalcBOLLDates4Stock();
        while(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("calcDate: {}", calcDate);
                mapper.genBOLLData4Stock(calcDate);
            }
            calcDates = mapper.getCalcBOLLDates4Stock();
        }

        log.info("historyAllStock end...");
        return BaseResponse.OK;
    }

    @GetMapping("/history-all/etf")
    public BaseResponse historyAllEtf() {
        log.info("historyAllEtf start...");

        List<String> calcDates = mapper.getCalcBOLLDates4Etf();
        while(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("calcDate: {}", calcDate);
                mapper.genBOLLData4Etf(calcDate);
            }
            calcDates = mapper.getCalcBOLLDates4Etf();
        }

        log.info("historyAllEtf end...");
        return BaseResponse.OK;
    }
}

