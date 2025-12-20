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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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


    @Autowired
    private MyCaffeineCache myCaffeineCache;

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
        Map<String, QqNode> qqNodeMap = new LinkedHashMap<>();
        List<QqNode> qqNodeList = new ArrayList<>();
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
            String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);
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

                break;
//
//                List<List<Object>> pojos = eastmoneyRes.getData().get(zqdm).get("qfqday");
//                if(pojos == null) {
//                    pojos = eastmoneyRes.getData().get(zqdm).get("day");
//                }
//                if(pojos == null) {
//                    continue;
//                }
//                List<QqNode> tmpNodes = pojos.stream()
//                        .map(Hs300POJO::toVo2)
//                        .sorted(Comparator.comparing(QqNode::getDate))
//                        .collect(Collectors.toList());
//                tmpNodes.forEach(f -> f.setStockCode(entry.getKey()));
//
//                QqNode existsNode = dataCalcMapper.getMaxQqNode(entry.getKey());
//                if(existsNode != null) {
//                    tmpNodes = tmpNodes.stream()
//                            .filter(f -> f.getDate().compareTo(existsNode.getDate()) > 0).collect(Collectors.toList());
//                }
//
//                if(!CollectionUtils.isEmpty(tmpNodes)) {
//                    for(int i = 0; i < tmpNodes.size(); i++) {
//                        QqNode currNode = tmpNodes.get(i);
//                        currNode.setStockCode(entry.getKey());
//                        if(0 == i) {
//                            if(existsNode == null) {
//                                currNode.setExpma5(currNode.getLast());
//                                currNode.setExpma10(currNode.getLast());
//                                currNode.setExpma20(currNode.getLast());
//                                currNode.setExpma37(currNode.getLast());
//                                currNode.setExpma60(currNode.getLast());
//                            } else {
//                                // private double calcExpma(double step, double lastValue, double cp) {
//                                currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
//                                currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
//                                currNode.setExpma20(calcExpma(20.0, existsNode.getExpma20(), currNode.getLast()));
//                                currNode.setExpma37(calcExpma(37.0, existsNode.getExpma37(), currNode.getLast()));
//                                currNode.setExpma60(calcExpma(60.0, existsNode.getExpma60(), currNode.getLast()));
//                            }
//                        } else {
//                            QqNode lastNode = tmpNodes.get(i - 1);
//                            currNode.setExpma5(calcExpma(5.0, lastNode.getExpma5(), currNode.getLast()));
//                            currNode.setExpma10(calcExpma(10.0, lastNode.getExpma10(), currNode.getLast()));
//                            currNode.setExpma20(calcExpma(20.0, lastNode.getExpma20(), currNode.getLast()));
//                            currNode.setExpma37(calcExpma(37.0, lastNode.getExpma37(), currNode.getLast()));
//                            currNode.setExpma60(calcExpma(60.0, lastNode.getExpma60(), currNode.getLast()));
//                        }
//                    }
//                    // 把最新的数据拿出来
//                    qqNodeMap.put(entry.getKey(), tmpNodes.get(tmpNodes.size() - 1));
//                    qqNodeList.addAll(tmpNodes);
//                }
            } catch (Exception ex) {
                log.error("", ex);
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

