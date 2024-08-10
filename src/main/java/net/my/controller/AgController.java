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
    @DeleteMapping("/remove/{time}")
    public BaseResponse remove(@CurrentUser UserBase userBase, @PathVariable("time") String time) {
        log.info("userBase: {}", userBase);
        log.info("remove-time:{}", time);
        dataCalc.deleteCP(time);
        calc(time);
        return RestGeneralResponse.of("删除完成");
    }

    @GetMapping("/data/cp/{time}")
    public BaseResponse queryCP(@PathVariable("time") String time) {
        List<AgClosePriceBO> bos = dataCalc.queryCP(time);

        Map<String, Integer> retMap = new LinkedHashMap<>();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> retMap.put(f.getName(), f.getClosePrice().intValue())
            );
        }

        return RestGeneralResponse.of(retMap);
    }

    @GetMapping("/data/cnt")
    public BaseResponse queryDataCnt() {
        List<AgDataCntBO> bos = dataCalc.queryDataCnt();

        return RestGeneralResponse.of(bos);
    }

    @GetMapping("/data/calc/{time}")
    public BaseResponse queryDataCalc(@PathVariable("time") String time) {
        List<AgDataCalcBO> bos = dataCalc.queryDataCalc(time);

        List<String> retList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(bos)) {
            retList = bos.stream().map(m -> String.format("%s: expma5=%.0f, expma37=%.0f, sRatio=%.4f(para=%.3f, pre=%.4f), bRatio=%.4f(para=%.3f, pre=%.4f)"
                    , m.getName(), m.getExpma5(), m.getExpma37(), m.getSRatio(), m.getSRatioPara(), m.getSRatioPre(), m.getBRatio(), m.getBRatioPara(), m.getBRatioPre())).collect(Collectors.toList());
        }

        return RestGeneralResponse.of(retList);
    }

    @GetMapping("/expect/hard/{change}")
    public BaseResponse expectHard(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectHard: time={}, change={}", time, change);
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
            BaseResponse response = queryHardOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    @GetMapping("/expect/simple/{change}")
    public BaseResponse expectSimple(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectSimple: time={}, change={}", time, change);
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
            BaseResponse response = querySimpleOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    @PostMapping("/add")
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

    @GetMapping("/oper/hard")
    public BaseResponse queryHardOper() {
        log.info("queryHardOper.");
        List<AgOper> opers = dataCalc.queryHardOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }

        return RestGeneralResponse.of(makeMap(opers));
    }

    @GetMapping("/oper/simple")
    public BaseResponse querySimpleOper() {
        log.info("querySimpleOper.");
        List<AgOper> opers = dataCalc.querySimpleOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }

        return RestGeneralResponse.of(makeMap(opers));
    }

    private Map<String, Map<String, List>> makeMap(List<AgOper> opers) {
        Map<String, Map<String, List>> retMap = new LinkedHashMap<>();
        if(CollectionUtils.isEmpty(opers)) {
            return retMap;
        }

        Comparator<String> comp = (String::compareTo);
        List<String> times = opers.stream().map(AgOper::getTime).distinct().sorted(comp.reversed()).collect(Collectors.toList());
        if(!times.contains("9999-99-99")) {
            retMap.put("预期操作", null);
        }
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
                if("9999-99-99".equals(time)) {
                    retMap.put("预期操作", timeMap);
                } else {
                    retMap.put(time, timeMap);
                }
            }
        }
        return retMap;
    }

}
