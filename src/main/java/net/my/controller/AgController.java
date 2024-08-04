package net.my.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.AgClosePriceBO;
import net.my.pojo.AgClosePriceDTO;
import net.my.pojo.AgDataCalcBO;
import net.my.pojo.AgOper;
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

    @DeleteMapping("/remove")
    public String remove(@RequestParam String time) {
        log.info("remove-time:{}", time);
        dataCalc.deleteCP(time);
        calc(time);
        return "删除完成";
    }

    @GetMapping("/queryCP")
    public String queryCP(@RequestParam String time) {
        List<AgClosePriceBO> bos = dataCalc.queryCP(time);
        StringBuilder res = new StringBuilder();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> res.append(f.getName()).append(": ").append(f.getClosePrice()).append("\r\n")
            );
        }
        return res.toString();
    }

    @GetMapping("/queryDataCalc")
    public String queryDataCalc(@RequestParam String time) {
        List<AgDataCalcBO> bos = dataCalc.queryDataCalc(time);
        StringBuilder res = new StringBuilder();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> res.append(f.getName()).append(": expma5=").append(f.getExpma5()).append(", expma37=").append(f.getExpma37()).append("\r\n")
            );
        }
        return res.toString();
    }

    @PostMapping("/add-data")
    public String addData(@RequestBody AgClosePriceDTO agClosePriceDTO) {
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
            return "数据添加完成";
        } else {
            return "无数据";
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
    public String queryOper() {
        log.info("queryOper");
        List<AgOper> opers = dataCalc.queryOper();
        if(CollectionUtils.isEmpty(opers)) {
            return "无操作";
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

        return stringBuilder.toString();
    }

}
