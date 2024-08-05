package net.my.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.my.interceptor.CurrentUser;
import net.my.interceptor.LoginRequired;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ag")
@Slf4j
public class AgController {

    @Autowired
    private DataCalcMapper dataCalc;

    @LoginRequired
    @DeleteMapping("/remove")
    public BaseResponse remove(@CurrentUser UserBase userBase, @RequestParam String time) {
        log.info("userBase: {}", userBase);
        log.info("remove-time:{}", time);
        dataCalc.deleteCP(time);
        calc(time);
        return RestGeneralResponse.of("删除完成");
    }

    @GetMapping("/query-cp")
    public BaseResponse queryCP(@RequestParam String time) {
        List<AgClosePriceBO> bos = dataCalc.queryCP(time);

        Map<String, Integer> retMap = new LinkedHashMap<>();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> retMap.put(f.getName(), f.getClosePrice().intValue())
            );
        }

        return RestGeneralResponse.of(retMap);
    }

    @GetMapping("/query-data-calc")
    public BaseResponse queryDataCalc(@RequestParam String time) {
        List<AgDataCalcBO> bos = dataCalc.queryDataCalc(time);

        List<String> retList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(bos)) {
            retList = bos.stream().map(m -> String.format("%s: expma5=%.0f, expma37=%.0f", m.getName(), m.getExpma5(), m.getExpma37())).collect(Collectors.toList());
        }

        return RestGeneralResponse.of(retList);
    }

    @GetMapping("/expect")
    public BaseResponse expect(@RequestParam("time") String time, @RequestParam("change") Double change) {
        log.info("addExpectData: time={}, change={}", time, change);
        if(dataCalc.getMaxTime().compareTo(time) >= 0) {
            return RestGeneralResponse.of(String.format("已存在日期大于或等于 %s 的数据，无需预测~", time));
        }

        List<AgClosePriceBO> agClosePriceBOList = dataCalc.getExpectCP(time, change);
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        dataCalc.insertCP(f);
                    }
            );

            calc(time);
            BaseResponse response = queryOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    @PostMapping("/add-data")
    public BaseResponse addData(@RequestBody AgClosePriceDTO agClosePriceDTO) {
        log.info("agClosePriceDTO:{}", JSON.toJSONString(agClosePriceDTO));
        List<AgClosePriceBO> agClosePriceBOList = agClosePriceDTO.toBO();
        agClosePriceBOList = agClosePriceBOList.stream().filter(f -> !StringUtils.isEmpty(f.getClosePrice())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        // 先删后插
                        dataCalc.deleteOneCP(f.getTime(), f.getType());
                        dataCalc.insertCP(f);
                    }
            );

            calc(agClosePriceDTO.getTime());
            return RestGeneralResponse.of("数据添加完成");
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    private void calc(String time) {
        // 删掉比插入的数据时间大的所有数据
        dataCalc.deleteDataCalcAfter(time);

        // 重新计算后续所有的数据
        int times = dataCalc.queryCalcTimes();

        log.info("times:{}", times);
        for(int i = 0; i < times; i++) {
            dataCalc.insertDataCalc();
        }

    }

    @GetMapping("/query-oper")
    public BaseResponse queryOper() {
        log.info("queryOper");
        List<AgOper> opers = dataCalc.queryOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }
        StringBuilder stringBuilder = new StringBuilder();
        Comparator<String> comp = (String::compareTo);
        List<String> times = opers.stream().map(AgOper::getTime).distinct().sorted(comp.reversed()).collect(Collectors.toList());
        Map<String, Map<String, List>> retMap = new LinkedHashMap<>();
        for(String time : times) {
            Map<String, List> timeMap = new LinkedHashMap<>();
            List<String> buyList = opers.stream().filter(f -> time.equals(f.getTime()) && "buy".equals(f.getOperDir()))
                    .map(m -> m.getName() + ": " + m.getBuyOper().replaceAll("buy", "买")).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(buyList)) {
                timeMap.put("买:", buyList);
            }

            List<String> sellList = opers.stream().filter(f -> time.equals(f.getTime()) && "sell".equals(f.getOperDir()))
                    .map(m -> m.getName() + ": " + m.getSellOper().replaceAll("sell", "卖")).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(sellList)) {
                timeMap.put("卖:", sellList);
            }

            if(!CollectionUtils.isEmpty(timeMap)) {
                retMap.put(time, timeMap);
            }
        }

        return RestGeneralResponse.of(retMap);
    }

}
