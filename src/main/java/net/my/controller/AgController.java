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

import java.util.Comparator;
import java.util.List;
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
        StringBuilder res = new StringBuilder();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> res.append(f.getName()).append(": ").append(f.getClosePrice()).append("\r\n")
            );
        }
        return RestGeneralResponse.of(res.toString());
    }

    @GetMapping("/query-data-calc")
    public BaseResponse queryDataCalc(@RequestParam String time) {
        List<AgDataCalcBO> bos = dataCalc.queryDataCalc(time);
        StringBuilder res = new StringBuilder();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> res.append(f.getName()).append(": expma5=").append(f.getExpma5()).append(", expma37=").append(f.getExpma37()).append("\r\n")
            );
        }
        return RestGeneralResponse.of(res.toString());
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
        for(String time : times) {
            stringBuilder.append(time)
                    .append(":\r\n");
            List<AgOper> opersBuy = opers.stream().filter(f -> time.equals(f.getTime()) && "buy".equals(f.getOperDir())).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(opersBuy)) {
                stringBuilder.append("买操作:\r\n");
                for(AgOper ag : opersBuy) {
                    stringBuilder.append(ag.getName()).append(": ").append(ag.getBuyOper()).append("\r\n");
                }
            }
            List<AgOper> opersSell = opers.stream().filter(f -> time.equals(f.getTime()) && "sell".equals(f.getOperDir())).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(opersSell)) {
                stringBuilder.append("卖操作:\r\n");
                for(AgOper ag : opersSell) {
                    stringBuilder.append(ag.getName()).append(": ").append(ag.getSellOper()).append("\r\n");
                }
            }
            stringBuilder.append("\r\n");
        }

        return RestGeneralResponse.of(stringBuilder.toString());
    }

}
