package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-new")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewController {

    static Map<String, String> PROXY_FINANCE_QQ = new LinkedHashMap<>();
    public static final String PROXY_FINANCE_QQ_URL_FORMAT = "https://proxy.finance.qq.com/cgi/cgi-bin/stockinfoquery/kline/app/get?code=%s&ktype=day&limit=%d";
    // demo
//    public static final String URL_FORMAT = "https://proxy.finance.qq.com/cgi/cgi-bin/stockinfoquery/kline/app/get?code=sh512880&ktype=day&limit=500";

    static {
        PROXY_FINANCE_QQ.put("恒生科技ETF-513130", "sh513130");
        PROXY_FINANCE_QQ.put("医疗创新EFT-516820", "sh516820");
        PROXY_FINANCE_QQ.put("上证50EFT-510050", "sh510050");
        PROXY_FINANCE_QQ.put("科创50EFT-588000", "sh588000");
        PROXY_FINANCE_QQ.put("沪深300EFT-510300", "sh510300");
        PROXY_FINANCE_QQ.put("光伏EFT-515790", "sh515790");
        PROXY_FINANCE_QQ.put("纳斯达克100EFT-159659", "sz159659");
        PROXY_FINANCE_QQ.put("华宝油气LOF-162411", "sz162411");
        PROXY_FINANCE_QQ.put("空天军工LOF-160643", "sz160643");
        PROXY_FINANCE_QQ.put("科创芯片EFT-588200", "sh588200");
        PROXY_FINANCE_QQ.put("煤炭ETF-515220", "sh515220");
        PROXY_FINANCE_QQ.put("有色金属ETF-512400", "sh512400");
        PROXY_FINANCE_QQ.put("交通银行-601328", "sh601328");
        PROXY_FINANCE_QQ.put("农业银行-601288", "sh601288");
        PROXY_FINANCE_QQ.put("工商银行-601398", "sh601398");
        PROXY_FINANCE_QQ.put("中国银行-601988", "sh601988");
        PROXY_FINANCE_QQ.put("邮储银行-601658", "sh601658");
        PROXY_FINANCE_QQ.put("京沪高铁-601816", "sh601816");

        PROXY_FINANCE_QQ.put("证券ETF-512880", "sh512880");
        PROXY_FINANCE_QQ.put("石油天然气ETF-159588", "sz159588");
        PROXY_FINANCE_QQ.put("A500ETF-159339", "sz159339");
        PROXY_FINANCE_QQ.put("软件ETF-515230", "sh515230");
        PROXY_FINANCE_QQ.put("基建ETF-516950", "sh516950");
        PROXY_FINANCE_QQ.put("国证2000ETF-159628", "sz159628");
        PROXY_FINANCE_QQ.put("黄金股ETF-517520", "sh517520");
        PROXY_FINANCE_QQ.put("白酒基金LOF-161725", "sz161725");
        PROXY_FINANCE_QQ.put("中证1000ETF增强-561280", "sh561280");
        PROXY_FINANCE_QQ.put("农业ETF-159825", "sz159825");
        PROXY_FINANCE_QQ.put("游戏ETF-159869", "sz159869");
        PROXY_FINANCE_QQ.put("电池ETF-159755", "sz159755");
    }

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataCalcMapper dataCalcMapper;

    @Autowired
    private RestTemplate restTemplate;


    private Double getScaleDouble(Double dou, int scale) {
        BigDecimal bd = new BigDecimal(dou);
        return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    @ApiOperation(value = "获取历史的cp数据", notes = "访问互联网接口获取数据")
    @ApiImplicitParam(name = "days", value = "制定历史上最近N天的数据", required = true, dataType = "String")
    @GetMapping("/history/{days}")
    @Transactional
    public BaseResponse getHistoryData(@PathVariable("days") Integer days) {
        List<AgClosePriceDTO> agClosePriceDTOs = new ArrayList<>();
        Map<String, QqNode> qqNodeMap = new LinkedHashMap<>();
        List<QqNode> qqNodeList = new ArrayList<>();
        for(Map.Entry<String, String> entry : PROXY_FINANCE_QQ.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(PROXY_FINANCE_QQ_URL_FORMAT, zqdm, days);
            log.info("url: {}, zqdm: {}, days: {}", url, zqdm, days);
            String res = "";
            for(int i = 0; i < 200; i++) {
                try {
                    Thread.sleep(200);
                    log.info("try num={}, stockCode={}, url={}", i, entry.getKey(), url);
                    res = restTemplate.getForObject(url, String.class);
                    if(!StringUtils.isEmpty(res)) {
                        break;
                    }
                } catch (Exception ex) {
                    ;
                }
            }
            if(StringUtils.isEmpty(res)) {
                continue;
            }

            try {
                QqRes qqRes = JSON.parseObject(res, QqRes.class);
                List<QqNode> tmpNodes = qqRes.getData().getNodes().stream()
                        .sorted(Comparator.comparing(QqNode::getDate)).collect(Collectors.toList());

                QqNode existsNode = dataCalcMapper.getMaxQqNode(entry.getKey());
                if(existsNode != null) {
                    tmpNodes = tmpNodes.stream()
                            .filter(f -> f.getDate().compareTo(existsNode.getDate()) > 0).collect(Collectors.toList());
                }

                if(!CollectionUtils.isEmpty(tmpNodes)) {
                    for(int i = 0; i < tmpNodes.size(); i++) {
                        QqNode currNode = tmpNodes.get(i);
                        currNode.setStockCode(entry.getKey());
                        if(0 == i) {
                            if(existsNode == null) {
                                currNode.setExpma5(currNode.getLast());
                                currNode.setExpma10(currNode.getLast());
                                currNode.setExpma20(currNode.getLast());
                                currNode.setExpma37(currNode.getLast());
                                currNode.setExpma60(currNode.getLast());
                            } else {
                                // private double calcExpma(double step, double lastValue, double cp) {
                                currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
                                currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
                                currNode.setExpma20(calcExpma(20.0, existsNode.getExpma20(), currNode.getLast()));
                                currNode.setExpma37(calcExpma(37.0, existsNode.getExpma37(), currNode.getLast()));
                                currNode.setExpma60(calcExpma(60.0, existsNode.getExpma60(), currNode.getLast()));
                            }
                        } else {
                            QqNode lastNode = tmpNodes.get(i - 1);
                            currNode.setExpma5(calcExpma(5.0, lastNode.getExpma5(), currNode.getLast()));
                            currNode.setExpma10(calcExpma(10.0, lastNode.getExpma10(), currNode.getLast()));
                            currNode.setExpma20(calcExpma(20.0, lastNode.getExpma20(), currNode.getLast()));
                            currNode.setExpma37(calcExpma(37.0, lastNode.getExpma37(), currNode.getLast()));
                            currNode.setExpma60(calcExpma(60.0, lastNode.getExpma60(), currNode.getLast()));
                        }
                    }
                    // 把最新的数据拿出来
                    qqNodeMap.put(entry.getKey(), tmpNodes.get(tmpNodes.size() - 1));
                    qqNodeList.addAll(tmpNodes);
                }
            } catch (Exception ex) {
                log.error("{}", ex);
            }
        }

        int startNum = 0;
        int stepNum = 100;
//        log.info("qqNodeList={}", JSON.toJSON(qqNodeList));
        while(startNum < qqNodeList.size()) {
            List<QqNode> tmpNodes = qqNodeList.stream().skip(startNum).limit(stepNum).collect(Collectors.toList());
            log.info("tmpNodes.size={}", tmpNodes.size());
            dataCalcMapper.saveQqNodes(tmpNodes);
            startNum += stepNum;
        }

//        qqNodeMap.values().forEach(qq -> dataCalcMapper.saveQqNode(qq));
        return RestGeneralResponse.of(qqNodeMap);
    }

    @GetMapping("/special-care/{time}")
    public BaseResponse specialCare(@PathVariable("time") String time) {
        log.info("specialCare.");
        List<SpecialCarePoJo> opers = dataCalcMapper.specialCare(time);

        return RestGeneralResponse.of(opers);
    }

    @GetMapping("/special-care-days/{swing}/{days}")
    public BaseResponse specialCareDays(@PathVariable("swing") String swing, @PathVariable("days") String days) {
        log.info("specialCare.");
        if(StringUtils.isEmpty(swing)) {
            swing = "-1";
        } else {
            swing = swing.trim();
            if(StringUtils.isEmpty(swing)) {
                swing = "-1";
            }
        }

        if(StringUtils.isEmpty(days)) {
            days = "30";
        } else {
            days = days.trim();
            if(StringUtils.isEmpty(days)) {
                days = "30";
            }
        }

        List<String> times = dataCalcMapper.getLatestDates(Integer.parseInt(days));
        List<SpecialCarePoJo> res = new ArrayList<>();
        for(String time : times) {
            // 查询表是否有数据
            List<SpecialCarePoJo> selectExistedData = dataCalcMapper.selectExistedData(time);
            if(CollectionUtils.isEmpty(selectExistedData)) {
                // 没有数据则重新计算
                List<SpecialCarePoJo> opers = dataCalcMapper.specialCare(time);
                opers = opers.stream().filter(f -> f.getRatioB() < 0.15).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(opers)) {
                    res.addAll(opers);
                    opers.forEach(f -> dataCalcMapper.insertSpecialData(f));
                } else {
                    SpecialCarePoJo pojo = new SpecialCarePoJo();
                    pojo.setDate(time);
                    dataCalcMapper.insertSpecialData(pojo);
                }
            } else {
                selectExistedData = selectExistedData.stream().filter(f -> !StringUtils.isEmpty(f.getStockCode())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(selectExistedData)) {
                    res.addAll(selectExistedData);
                }
            }
        }

        List<SpecialCarePoJo> opersExpect = dataCalcMapper.expectSpcialCare(Double.parseDouble(swing));
        opersExpect = opersExpect.stream().filter(f -> f.getRatioB() < 0.15).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(opersExpect)) {
            SpecialCarePoJo pojo = new SpecialCarePoJo();
            pojo.setDate("T+1");
            pojo.setStockCode("无数据");
            res.add(pojo);
        } else {
            res.addAll(opersExpect);
        }

        res = res.stream().sorted(Comparator.comparing(SpecialCarePoJo::getDate, Comparator.reverseOrder())
                .thenComparing(SpecialCarePoJo::getStockCode)).collect(Collectors.toList());

        return RestGeneralResponse.of(res);
    }

    private double calcExpma(double step, double lastValue, double cp) {
        return (cp - lastValue) * 2.0 / (step + 1) + lastValue;
        // round((t.close_price - t3.`expma_5`)*2.0/(5.0+1) + t3.`expma_5`, 6)
//        BigDecimal bigDecimalLastValue = BigDecimal.valueOf(lastValue);
//        BigDecimal bigDecimalCp = BigDecimal.valueOf(cp);
//        return bigDecimalLastValue.add(
//                (bigDecimalCp.subtract(bigDecimalLastValue))
//                        .multiply(new BigDecimal(2.0))
//                        .divide(new BigDecimal(step + 1))
//        )
//                .doubleValue();
    }


    @Data
    public class QqRes{
        private Integer code;
        private String message;
        private QqData data;
    }

    @Data
    public class QqData{
        private List<QqNode> nodes;
    }

}
