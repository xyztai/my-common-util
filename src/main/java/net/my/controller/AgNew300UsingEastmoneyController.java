package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-eastmoney-hs300")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNew300UsingEastmoneyController {

    // demo: "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600276&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61";
    // fqt=1 表示前复权
    public static final String EASTMONEY_URL_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    public static final String EASTMONEY_URL_BEGIN_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=%s&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";


    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataCalcMapper dataCalcMapper;

    @Autowired
    private RestTemplate restTemplate;


    @ApiOperation(value = "获取历史的cp数据", notes = "访问互联网接口获取数据")
    @ApiImplicitParam(name = "days", value = "制定历史上最近N天的数据", required = true, dataType = "String")
    @GetMapping("/historyAll")
    @Transactional
    public BaseResponse getHistoryData() {
        List<AgClosePriceDTO> agClosePriceDTOs = new ArrayList<>();
        List<EastmoneyNode> eastmoneyNodeList = new ArrayList<>();
        Map<String, EastmoneyNode> eastmoneyNodeMap = new LinkedHashMap<>();
        List<Hs300PO> hs300List = dataCalcMapper.getHs300List();

        if(CollectionUtils.isEmpty(hs300List)) {
            return BaseResponse.OK;
        }

        Map<String, String> hs300Map = new LinkedHashMap<>();
        for(Hs300PO po : hs300List) {
            String key = "3_" + po.getStockName() + "-" + po.getStockCode();
            String value = 0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode();
            hs300Map.put(key, value);
        }


        for(Map.Entry<String, String> entry : hs300Map.entrySet()) {
            String zqdm = entry.getValue();
//            if(!zqdm.equals("0.000001")) {
//                continue;
//            }
            
            String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);

            EastmoneyNode existsNode = dataCalcMapper.getMaxEastMoneyNode(zqdm);
            if(existsNode != null) {
                url = String.format(EASTMONEY_URL_BEGIN_FORMAT_QFQ, zqdm, existsNode.getDate().replaceAll("-", ""));
            }

            log.info("url: {}, zqdm: {}", url, zqdm);
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
                log.info("res={}", res);
                Hs300EastmoneyRes eastmoneyRes = JSON.parseObject(res, Hs300EastmoneyRes.class);
                log.info("eastmoneyRes={}", JSON.toJSONString(eastmoneyRes));

//                break;

                // saveEastMoneyDatas
                if(eastmoneyRes == null || eastmoneyRes.getData() == null || CollectionUtils.isEmpty(eastmoneyRes.getData().getKlines())) {
                    break ;
                }

                List<String> klines = eastmoneyRes.getData().getKlines();
                List<EastmoneyNode> nodes = new ArrayList<>();
                for(String item : klines) {
                    String[] xxs = item.split(",");
                    nodes.add(EastmoneyNode.builder().date(xxs[0]).stockCode(zqdm).infoRaw(item).build());
                }


                if(existsNode != null) {
                    nodes = nodes.stream()
                            .filter(f -> f.getDate().compareTo(existsNode.getDate()) > 0).collect(Collectors.toList());
                }

                if(!CollectionUtils.isEmpty(nodes)) {
                    eastmoneyNodeMap.put(entry.getKey(), nodes.get(nodes.size() - 1));
                    eastmoneyNodeList.addAll(nodes);
                }
            } catch (Exception ex) {
                log.error("", ex);
            }
        }

        int startNum = 0;
        int stepNum = 100;
        while(startNum < eastmoneyNodeList.size()) {
            List<EastmoneyNode> tmpNodes = eastmoneyNodeList.stream().skip(startNum).limit(stepNum).collect(Collectors.toList());
            log.info("tmpNodes.size={}", tmpNodes.size());
            dataCalcMapper.saveEastMoneyDatas(tmpNodes);
            startNum += stepNum;
        }

        log.info("阶段1-非99999数据-开始更新基础字段");
        // 更新基础字段
        dataCalcMapper.updateEastMoneyDatas();
        // 更新expma字段
        log.info("阶段1-非99999数据-开始更新expma字段");
        updateExpma();
        // 删除 预期数据
        log.info("阶段2-99999数据-deleteExpect99999");
        dataCalcMapper.deleteExpect99999();
        // 开始插入 预期数据
        log.info("阶段2-99999数据-insertExpect2099");
        dataCalcMapper.insertExpect99999();
        log.info("阶段2-99999数据-开始更新基础字段");
        // 更新基础字段
        dataCalcMapper.updateEastMoneyDatas();
        // 更新expma字段
        log.info("阶段2-99999数据-开始更新expma字段");
        updateExpma();
        log.info("删除 delEastMoneyBuy99999");
        dataCalcMapper.delEastMoneyBuy99999();
        // 更新buy表
        List<String> needCalcBuys = dataCalcMapper.getNeedCalcDates("2025-06-01", "20");
        if(!CollectionUtils.isEmpty(needCalcBuys)) {
            for(String currDate : needCalcBuys) {
                log.info("calcBuy currDate={}", currDate);
                dataCalcMapper.saveEastmoneyNodeBuys(currDate);
            }
        }

//        qqNodeMap.values().forEach(qq -> dataCalcMapper.saveQqNode(qq));
        return RestGeneralResponse.of(eastmoneyNodeMap);
    }

    @GetMapping("/insert99999")
    public BaseResponse insert99999() {
        // 删除 预期数据
        log.info("阶段2-99999数据-deleteExpect99999");
        dataCalcMapper.deleteExpect99999();
        // 开始插入 预期数据
        log.info("阶段2-99999数据-insertExpect2099");
        dataCalcMapper.insertExpect99999();
        log.info("阶段2-99999数据-开始更新基础字段");
        // 更新基础字段
        dataCalcMapper.updateEastMoneyDatas();
        // 更新expma字段
        log.info("阶段2-99999数据-开始更新expma字段");
        updateExpma();
        log.info("删除 delEastMoneyBuy99999");
        dataCalcMapper.delEastMoneyBuy99999();
        // 更新buy表
        List<String> needCalcBuys = dataCalcMapper.getNeedCalcDates("2025-06-01", "3");
        if(!CollectionUtils.isEmpty(needCalcBuys)) {
            for(String currDate : needCalcBuys) {
                log.info("calcBuy currDate={}", currDate);
                dataCalcMapper.saveEastmoneyNodeBuys(currDate);
            }
        }
        return BaseResponse.OK;
    }

    @GetMapping("/updateExpma")
    public BaseResponse updateExpma() {
        List<Hs300PO> hs300List = dataCalcMapper.getHs300List();
        if(CollectionUtils.isEmpty(hs300List)) {
            return BaseResponse.OK;
        }

        List<String> zqdms = new ArrayList<>();
        hs300List.forEach(po -> zqdms.add(0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode()));

        for(String zqdm : zqdms) {
//            String zqdm = "0.000001";
            List<EastmoneyNode> needUpdateExpmas = dataCalcMapper.getEastMoneyNodes(zqdm);
            if(CollectionUtils.isEmpty(needUpdateExpmas)) {
                continue;
            }

            log.info("needUpdateExpmas={}", JSON.toJSONString(needUpdateExpmas));
            needUpdateExpmas = needUpdateExpmas.stream().sorted(Comparator.comparing(EastmoneyNode::getDate)).collect(Collectors.toList());
            EastmoneyNode existsNode = dataCalcMapper.getMaxEastMoneyNodeHasExpma(zqdm);

            if(!CollectionUtils.isEmpty(needUpdateExpmas)) {
                for(int i = 0; i < needUpdateExpmas.size(); i++) {
                    EastmoneyNode currNode = needUpdateExpmas.get(i);
                    log.info("existsNode={}", JSON.toJSONString(existsNode));
                    log.info("currNode={}", JSON.toJSONString(currNode));
                    if(currNode.getDate().startsWith("99999")) {
                        continue;
                    }
                    if(0 == i) {
                        if(existsNode == null) {
                            currNode.setExpma5(currNode.getLast());
                            currNode.setExpma10(currNode.getLast());
                        } else {
                            currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
                            currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
                        }
                    } else {
                        EastmoneyNode lastNode = needUpdateExpmas.get(i - 1);
                        currNode.setExpma5(calcExpma(5.0, lastNode.getExpma5(), currNode.getLast()));
                        currNode.setExpma10(calcExpma(10.0, lastNode.getExpma10(), currNode.getLast()));
                    }
                    log.info("updateExpmaEastmoney currNode={}", JSON.toJSON(currNode));
                    dataCalcMapper.updateExpmaEastmoney(currNode);
                }

                needUpdateExpmas = needUpdateExpmas.stream().filter(f -> f.getDate().startsWith("99999")).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(needUpdateExpmas)) {
                    existsNode = dataCalcMapper.getMaxEastMoneyNodeHasExpma(zqdm);
                    if(existsNode != null) {
                        for(int i = 0; i < needUpdateExpmas.size(); i++) {
                            EastmoneyNode currNode = needUpdateExpmas.get(i);
                            currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
                            currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
                            log.info("updateExpmaEastmoney currNode={}", JSON.toJSON(currNode));
                            dataCalcMapper.updateExpmaEastmoney(currNode);
                        }
                    }
                }
            }
        }

        return BaseResponse.OK;
    }

    @GetMapping("/calcBuy")
    public BaseResponse calcBuy() {
        List<String> needCalcBuys = dataCalcMapper.getNeedCalcDates("2021-01-01", "2000");

        if(CollectionUtils.isEmpty(needCalcBuys)) {
            return BaseResponse.OK;
        }

        for(String currDate : needCalcBuys) {
            log.info("calcBuy currDate={}", currDate);
            dataCalcMapper.saveEastmoneyNodeBuys(currDate);
        }

        return BaseResponse.OK;
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
    public class Hs300EastmoneyRes{
        private Hs300EastmoneyPOJO data;
    }

    @Data
    public static class Hs300EastmoneyPOJO{
        /**
         * [
         *                     "2023-04-28", // 日期
         *                     "10.78",  // 开
         *                     "10.99",  // 收
         *                     "11.08",  // 高
         *                     "10.70",  // 低
         *                     "1975714.00", // 量
         *                     {},
         *                     "2.58", // 换手率
         *                     "233884.16", // 金额，单位 万元
         *                     ""
         *                 ]
         */
        private String code;
        private List<String> klines;
    }
}

