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
        PROXY_FINANCE_QQ.put("001纳斯达克100ETF-159659", "sz159659");
        PROXY_FINANCE_QQ.put("002纳指100ETF-159660", "sz159660");
        PROXY_FINANCE_QQ.put("003恒生科技ETF-513130", "sh513130");
        PROXY_FINANCE_QQ.put("004日经ETF-513520", "sh513520");
        PROXY_FINANCE_QQ.put("005沙特ETF-159329", "sz159329");
        PROXY_FINANCE_QQ.put("006华宝油气LOF-162411", "sz162411");
        PROXY_FINANCE_QQ.put("007香港证券ETF-513090", "sh513090");
        PROXY_FINANCE_QQ.put("008港股通非银ETF-513750", "sh513750");
        PROXY_FINANCE_QQ.put("009H股ETF-159954", "sz159954");
        PROXY_FINANCE_QQ.put("010黄金ETF-518880", "sh518880");
        PROXY_FINANCE_QQ.put("011有色ETF-159980", "sz159980");
        PROXY_FINANCE_QQ.put("101中证1000ETF增强-561280", "sh561280");
        PROXY_FINANCE_QQ.put("102国证2000ETF-159628", "sz159628");
        PROXY_FINANCE_QQ.put("103A500ETF-159339", "sz159339");
        PROXY_FINANCE_QQ.put("104证券ETF-512880", "sh512880");
        PROXY_FINANCE_QQ.put("105航空航天ETF-159227", "sz159227");
        PROXY_FINANCE_QQ.put("106电力ETF-159611", "sz159611");
        PROXY_FINANCE_QQ.put("107电网设备ETF-159326", "sz159326");
        PROXY_FINANCE_QQ.put("108石油天然气ETF-159588", "sz159588");
        PROXY_FINANCE_QQ.put("109储能电池ETF-159566", "sz159566");
        PROXY_FINANCE_QQ.put("110半导体设备ETF-159516", "sz159516");
        PROXY_FINANCE_QQ.put("111科创芯片ETF-588200", "sh588200");
        PROXY_FINANCE_QQ.put("112机器人ETF-562500", "sh562500");
        PROXY_FINANCE_QQ.put("113卫星ETF-159206", "sz159206");
        PROXY_FINANCE_QQ.put("114医疗创新ETF-516820", "sh516820");
        PROXY_FINANCE_QQ.put("115电池ETF-159755", "sz159755");
        PROXY_FINANCE_QQ.put("116军工ETF-512660", "sh512660");
        PROXY_FINANCE_QQ.put("117空天军工LOF-160643", "sz160643");
        PROXY_FINANCE_QQ.put("118黄金股ETF-517520", "sh517520");
        PROXY_FINANCE_QQ.put("119游戏ETF-159869", "sz159869");
        PROXY_FINANCE_QQ.put("120软件ETF-515230", "sh515230");
        PROXY_FINANCE_QQ.put("121光伏ETF-515790", "sh515790");
        PROXY_FINANCE_QQ.put("122科创50ETF-588000", "sh588000");
        PROXY_FINANCE_QQ.put("1235G通信ETF-515050", "sh515050");
        PROXY_FINANCE_QQ.put("124农业ETF-159825", "sz159825");
        PROXY_FINANCE_QQ.put("125基建ETF-516950", "sh516950");
        PROXY_FINANCE_QQ.put("126旅游ETF-159766", "sz159766");
        PROXY_FINANCE_QQ.put("127银行ETF天弘-515290", "sh515290");


//        PROXY_FINANCE_QQ.put("* 港股创新药ETF-159567", "sz159567");
//        PROXY_FINANCE_QQ.put("上证50ETF-510050", "sh510050");
//        PROXY_FINANCE_QQ.put("人工智能ETF-515980", "sh515980");
//        PROXY_FINANCE_QQ.put("医药ETF-512010", "sh512010");
//        PROXY_FINANCE_QQ.put("中国银行-601988", "sh601988");
//        PROXY_FINANCE_QQ.put("交通银行-601328", "sh601328");
//        PROXY_FINANCE_QQ.put("京沪高铁-601816", "sh601816");
//        PROXY_FINANCE_QQ.put("农业银行-601288", "sh601288");
//        PROXY_FINANCE_QQ.put("创业板成长ETF-159967", "sz159967");
//        PROXY_FINANCE_QQ.put("医疗ETF-512170", "sh512170");
//        PROXY_FINANCE_QQ.put("半导体ETF-512480", "sh512480");
//        PROXY_FINANCE_QQ.put("工商银行-601398", "sh601398");
//        PROXY_FINANCE_QQ.put("有色金属ETF-512400", "sh512400");
//        PROXY_FINANCE_QQ.put("沪深300ETF-510300", "sh510300");
//        PROXY_FINANCE_QQ.put("煤炭ETF-515220", "sh515220");
//        PROXY_FINANCE_QQ.put("白酒基金LOF-161725", "sz161725");
//        PROXY_FINANCE_QQ.put("邮储银行-601658", "sh601658");
//        PROXY_FINANCE_QQ.put("酒ETF-512690", "sh512690");
//        PROXY_FINANCE_QQ.put("银行ETF-512800", "sh512800");
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
        getSpecialCareDays("-1", "300");
        return RestGeneralResponse.of(qqNodeMap);
    }

    @GetMapping("/special-care/{time}")
    public BaseResponse specialCare(@PathVariable("time") String time) {
        log.info("specialCare.");
        List<SpecialCarePoJo> res = dataCalcMapper.specialCare(time);


        // 参数根据历史数据来获得
        List<Hs300Para> historyParas =  dataCalcMapper.getHistoryParas();
        List<SpecialCarePoJo> resPoJos = new ArrayList<>();
        for(SpecialCarePoJo item : res) {
            if(!item.getStockCode().startsWith("3_")) {
                resPoJos.add(item);
                continue;
            }

            List<Hs300Para> tmp = historyParas.stream()
                    .filter(f -> item.getStockCode().equals(f.getStockCode()) && item.getRatioB() <= f.getRatioB()).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(tmp)) {
                continue;
            }
            Integer rankR = tmp.stream().map(Hs300Para::getRankR).sorted().findFirst().get();
            item.setStockCode(item.getStockCode() + "-" + rankR);
            resPoJos.add(item);
        }

        return RestGeneralResponse.of(resPoJos);

//        return RestGeneralResponse.of(opers.stream().filter(f -> !f.getStockCode().startsWith("3_") || f.getRatioB() < 0.008)
//                .collect(Collectors.toList()));
    }

    private List<SpecialCarePoJo> getSpecialCareDays(String swing, String days) {
        log.info("special-care-days swing={}, days={}", swing, days);
        if(StringUtils.isEmpty(swing)) {
            swing = "-1";
        } else {
            swing = swing.trim();
            if(StringUtils.isEmpty(swing)) {
                swing = "-1";
            }
        }

        if(StringUtils.isEmpty(days)) {
            days = "300";
        } else {
            days = days.trim();
            if(StringUtils.isEmpty(days)) {
                days = "300";
            }
        }

        List<String> times = dataCalcMapper.getLatestDates(Integer.parseInt(days));
        List<SpecialCarePoJo> res = new ArrayList<>();
        for(String time : times) {
            log.info("special-care-days time={}", time);
            // 查询表是否有数据
            List<SpecialCarePoJo> selectExistedData = dataCalcMapper.selectExistedData(time);
            if(CollectionUtils.isEmpty(selectExistedData)) {
                // 没有数据则重新计算
                List<SpecialCarePoJo> opers = dataCalcMapper.specialCare(time);
//                opers = opers.stream().filter(f -> f.getRatioB() < 0.15).collect(Collectors.toList());
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
//        opersExpect = opersExpect.stream().filter(f -> f.getRatioB() < 0.15).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(opersExpect)) {
            SpecialCarePoJo pojo = new SpecialCarePoJo();
            pojo.setDate("T+1");
            pojo.setStockCode("无数据");
            res.add(pojo);
        } else {
            opersExpect.forEach(f -> f.setDate("T+1"));
            res.addAll(opersExpect);
        }

        // 参数根据历史数据来获得
        List<Hs300Para> historyParas =  dataCalcMapper.getHistoryParas();
        List<SpecialCarePoJo> resPoJos = new ArrayList<>();
        for(SpecialCarePoJo item : res) {
            List<Hs300Para> tmp = historyParas.stream()
                    .filter(f -> item.getStockCode().equals(f.getStockCode()) && item.getRatioB() <= f.getRatioB()).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(tmp)) {
                continue;
            }
            Integer rankR = tmp.stream().map(Hs300Para::getRankR).sorted().findFirst().get();
            item.setStockCode(item.getStockCode() + "-" + rankR);
            resPoJos.add(item);
        }

//        // 参数设置：T+0的为0.10，T+1的为0.05
//        List<SpecialCarePoJo> resPoJos = new ArrayList<>();
//        resPoJos.addAll(res.stream()
//                .filter(f -> f.getStockCode().startsWith("0") && f.getRatioB() < 0.10)
//                .collect(Collectors.toList()));
//        resPoJos.addAll(res.stream()
//                .filter(f -> f.getStockCode().startsWith("1") && f.getRatioB() < 0.05)
//                .collect(Collectors.toList()));
//        resPoJos.addAll(res.stream()
//                .filter(f -> f.getStockCode().startsWith("3_") && f.getRatioB() < 0.008)
//                .collect(Collectors.toList()));
        resPoJos = resPoJos.stream().sorted(Comparator.comparing(SpecialCarePoJo::getDate, Comparator.reverseOrder())
                .thenComparing(SpecialCarePoJo::getStockCode)).collect(Collectors.toList());

        List<SpecialCarePoJo> resPoJos2 = new ArrayList<>();

        Set<String> stockCodesT = resPoJos.stream().filter(f -> f.getDate().startsWith("T+1")).map(SpecialCarePoJo::getStockCode).collect(Collectors.toSet());
        resPoJos2.addAll(resPoJos.stream().filter(f -> stockCodesT.contains(f.getStockCode())).sorted(Comparator.comparing(SpecialCarePoJo::getStockCode).thenComparing(SpecialCarePoJo::getDate, Comparator.reverseOrder()))
                .collect(Collectors.toList()));
        resPoJos2.addAll(resPoJos.stream().filter(f -> !stockCodesT.contains(f.getStockCode())).collect(Collectors.toList()));

        return resPoJos2;
    }

    @GetMapping("/special-care-days/{swing}/{days}")
    public BaseResponse specialCareDays(@PathVariable("swing") String swing, @PathVariable("days") String days) {
        log.info("special-care-days swing={}, days={}", swing, days);
        if(StringUtils.isEmpty(swing)) {
            swing = "-1";
        } else {
            swing = swing.trim();
            if(StringUtils.isEmpty(swing)) {
                swing = "-1";
            }
        }

        if(StringUtils.isEmpty(days)) {
            days = "300";
        } else {
            days = days.trim();
            if(StringUtils.isEmpty(days)) {
                days = "300";
            }
        }

        String key = "special-care-days" + "#" + days;
        List<SpecialCarePoJo> cachedRes = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(cachedRes != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, cachedRes);
            return RestGeneralResponse.of(cachedRes);
        }

        List<SpecialCarePoJo> resPoJos2 = getSpecialCareDays(swing, days);
        resPoJos2 = resPoJos2.stream().filter(f -> !f.getStockCode().contains("-688") && !f.getStockCode().contains("-300")).collect(Collectors.toList());

        myCaffeineCache.put(key, resPoJos2);
        log.info("myCaffeineCache put, key={}, res={}", key, resPoJos2);
        return RestGeneralResponse.of(resPoJos2);
    }

    private double calcExpma(double step, double lastValue, double cp) {
        return (cp - lastValue) * 2.0 / (step + 1) + lastValue;
    }


    /**
     * 清空缓存
     */
    @GetMapping("/invalidateAll")
    void invalidateAll() {
        log.info("invalidateAll...");
        myCaffeineCache.invalidateAll();
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
