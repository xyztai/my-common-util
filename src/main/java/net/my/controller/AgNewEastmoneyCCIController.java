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
@RequestMapping("/ag-cci-eastmoney-stock")
@Slf4j
public class AgNewEastmoneyCCIController {

    @Autowired
    private AgCCIEastmoneyStockMapper mapper;

    @GetMapping("/history-all")
    public BaseResponse historyAll() {
        log.info("historyAll start...");

        List<String> calcDates = mapper.getCalcCCIDates();
        if(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("calcDate: {}", calcDate);
                mapper.genCCIData(calcDate);
            }
        }

        log.info("historyAll end...");
        return BaseResponse.OK;
    }
}

