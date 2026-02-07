package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.TmpMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/avg")
@Slf4j
public class TmpController {

    @Autowired
    private TmpMapper tmpMapper;

    @GetMapping("/calc/{calcDate}")
    public BaseResponse calc(@PathVariable("calcDate") String calcDate) {
        log.info("calc start");
        List<String> dates = tmpMapper.getDates(calcDate);
        if(!CollectionUtils.isEmpty(dates)) {
            for(String d : dates) {
                log.info("calc calcD={}", calcDate);
                tmpMapper.calcAvg(d);
            }
        }
        log.info("calc end");
        return RestGeneralResponse.OK;
    }
}
