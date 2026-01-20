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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-new-hs300")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewQQ300Controller {
    /**
     * 通过下面的3个接口，获取得到沪深300的列表，其中 f13 = 0 表示深圳；f13 = 1 表示上海
     *
     * https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104410682373325254_1765732657463&fs=b%3Abk0500%2Bf%3A!50&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f3&pn=1&pz=100&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1765732657486
     * https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104410682373325254_1765732657463&fs=b%3Abk0500%2Bf%3A!50&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f3&pn=2&pz=100&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1765732657486
     * https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104410682373325254_1765732657463&fs=b%3Abk0500%2Bf%3A!50&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f3&pn=3&pz=100&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1765732657486
     */

    // 复权（通达信默认为前复权）
    public static final String PROXY_FINANCE_QQ_URL_FORMAT_QFQ
            = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?_var=kline_dayqfq&param=%s,day,,,%d,qfq";
    // demo
//    public static final String URL_FORMAT_QFQ
//    = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?_var=kline_dayqfq&param=sz000001,day,,,2000,qfq";


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
        List<HsStockPoJo> hs300List = dataCalcMapper.getHs300List();

        if(CollectionUtils.isEmpty(hs300List)) {
            return BaseResponse.OK;
        }

        Map<String, String> hs300Map = new LinkedHashMap<>();
        for(HsStockPoJo po : hs300List) {
            String key = "3_" + po.getStockName() + "-" + po.getStockCode();
            String value = 0 == po.getStockType() ? "sz" + po.getStockCode() : "sh" + po.getStockCode();
            hs300Map.put(key, value);
        }


        for(Map.Entry<String, String> entry : hs300Map.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(PROXY_FINANCE_QQ_URL_FORMAT_QFQ, zqdm, days);
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
                res = res.replace("kline_dayqfq=", "");//.replace(",{},", ",");
                res = res.substring(0, res.indexOf(",\"qt\"")) + "}}}";
                log.info("res={}", res);
                Hs300Res qqRes = JSON.parseObject(res, Hs300Res.class);
//                List<HsStockPoJoJO> pojos = qqRes.getData().get(zqdm).get("qfqday");
//                List<QqNode> tmpNodes = pojos.stream().map(HsStockPoJoJO::toVo).sorted(Comparator.comparing(QqNode::getDate)).collect(Collectors.toList());

                List<List<Object>> pojos = qqRes.getData().get(zqdm).get("qfqday");
                if(pojos == null) {
                    pojos = qqRes.getData().get(zqdm).get("day");
                }
                if(pojos == null) {
                    continue;
                }
                List<QqNode> tmpNodes = pojos.stream()
                        .map(HsStockPoJoJO::toVo2)
                        .sorted(Comparator.comparing(QqNode::getDate))
                        .collect(Collectors.toList());
                tmpNodes.forEach(f -> f.setStockCode(entry.getKey()));

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

    @GetMapping("/qq/{zqdm}/{days}")
    public BaseResponse getQQResReplaceEastmoneyOuter(@PathVariable("zqdm") String zqdm
                                                      , @PathVariable("days") Integer days) {
        return RestGeneralResponse.of(getQQResReplaceEastmoney(zqdm, days));
    }

    public List<String> getQQResReplaceEastmoney(String zqdm, Integer days) {
        List<String> klines = new ArrayList<>();
        // zqdm 举例，000001 为 sz000001
        String url = String.format(PROXY_FINANCE_QQ_URL_FORMAT_QFQ, zqdm, days);
        log.info("url: {}, zqdm: {}, days: {}", url, zqdm, days);
        String res = "";
        for(int i = 0; i < 10; i++) {
            try {
                Thread.sleep(200);
                log.info("try num={}, stockCode={}, url={}", i, zqdm, url);
                res = restTemplate.getForObject(url, String.class);
                if(!StringUtils.isEmpty(res)) {
                    break;
                }
            } catch (Exception ex) {
                ;
            }
        }
        if(StringUtils.isEmpty(res)) {
            return klines;
        }

        try {
            res = res.replace("kline_dayqfq=", "");//.replace(",{},", ",");
            res = res.substring(0, res.indexOf(",\"qt\"")) + "}}}";
            log.info("res={}", res);
            Hs300Res qqRes = JSON.parseObject(res, Hs300Res.class);
//                List<HsStockPoJoJO> pojos = qqRes.getData().get(zqdm).get("qfqday");
//                List<QqNode> tmpNodes = pojos.stream().map(HsStockPoJoJO::toVo).sorted(Comparator.comparing(QqNode::getDate)).collect(Collectors.toList());

            List<List<Object>> pojos = qqRes.getData().get(zqdm).get("qfqday");
            if(pojos == null) {
                pojos = qqRes.getData().get(zqdm).get("day");
            }
            if(pojos == null) {
                return klines;
            }
            List<QqNode> tmpNodes = pojos.stream()
                    .map(HsStockPoJoJO::toVo2)
                    .sorted(Comparator.comparing(QqNode::getDate))
                    .collect(Collectors.toList());
            Double preLast = tmpNodes.get(0).getLast();
            for(int i = 1; i < tmpNodes.size(); i++) {
                QqNode node = tmpNodes.get(i);
                String kline = HsStockPoJoJO.toEastMoneyData(node);
                // "zhen_fu_ratio" // 振幅=(当天的high-当天的low)/昨天的last*100
                // "zhang_fu_ratio" // 涨幅=(当天的last-昨天的last)/昨天的last*100
                // "zhang_fu_zhi" // 涨幅值=当天的last-昨天的last
                DecimalFormat df2 = new DecimalFormat("0.00");
                DecimalFormat df3 = new DecimalFormat("0.000");
                String zhenFuRatio = df2.format((node.getHigh() - node.getLow()) / preLast * 100);
                String zhangFuRatio = df2.format((node.getLast() - preLast) / preLast * 100);
                String zhangFuZhi = df3.format(node.getLast() - preLast);
                kline = kline.replace("zhen_fu_ratio", zhenFuRatio);
                kline = kline.replace("zhang_fu_ratio", zhangFuRatio);
                kline = kline.replace("zhang_fu_zhi", zhangFuZhi);
                klines.add(kline);
                preLast = node.getLast();
            }
            return klines;
        } catch (Exception ex) {
            log.error("", ex);
        }
        return new ArrayList<>();
    }

    @Data
    public class Hs300Res{
        private Integer code;
        private String msg;
        private Map<String, Map<String, List<List<Object>>>> data;
    }

    @Data
    public static class HsStockPoJoJO{
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
        private List<Object> multiStr;
        public QqNode toVo() {
            return QqNode.builder()
                    .date(multiStr.get(0).toString())
                    .open(Double.parseDouble(multiStr.get(1).toString()))
                    .last(Double.parseDouble(multiStr.get(2).toString()))
                    .high(Double.parseDouble(multiStr.get(3).toString()))
                    .low(Double.parseDouble(multiStr.get(4).toString()))
                    .volume(Double.parseDouble(multiStr.get(5).toString()))
                    .amount(Double.parseDouble(multiStr.get(7).toString()))
                    .exchangeRaw(Double.parseDouble(multiStr.get(6).toString()))
//                    .amount(Double.parseDouble(multiStr.get(8).toString()))
//                    .exchangeRaw(Double.parseDouble(multiStr.get(7).toString()))
                    .build();
        }
        public static QqNode toVo2(List<Object> objects) {
            return QqNode.builder()
                    .date(objects.get(0).toString())
                    .open(Double.parseDouble(objects.get(1).toString()))
                    .last(Double.parseDouble(objects.get(2).toString()))
                    .high(Double.parseDouble(objects.get(3).toString()))
                    .low(Double.parseDouble(objects.get(4).toString()))
                    .volume(Double.parseDouble(objects.get(5).toString()))
//                    .amount(Double.parseDouble(objects.get(7).toString()))
//                    .exchangeRaw(Double.parseDouble(objects.get(6).toString()))
                    .amount(Double.parseDouble(objects.get(8).toString()))
                    .exchangeRaw(Double.parseDouble(objects.get(7).toString()))
                    .build();
        }

        public static String toEastMoneyData(QqNode node) {
            DecimalFormat df = new DecimalFormat("0.00");
            return "" + node.getDate() // date
                    + "," + node.getOpen() // open
                    + "," + node.getLast() // last
                    + "," + node.getHigh() // high
                    + "," + node.getLow() // low
                    + "," + (long)(node.getVolume() * 1L) // volume
                    + "," + (long)(node.getAmount() * 10000L) // amount
                    + "," + "zhen_fu_ratio" // 振幅=(当天的high-当天的low)/昨天的last*100
                    + "," + "zhang_fu_ratio" // 涨幅=(当天的last-昨天的last)/昨天的last*100
                    + "," + "zhang_fu_zhi" // 涨幅值=当天的last-昨天的last
                    + "," + df.format(node.getExchangeRaw()) // 换手
                    ;
        }

        public static String toEastMoneyData(List<Object> objects) {
            // "2026-01-20,11.12,11.16,11.20,11.11,772276,861694177.62,0.81,0.36,0.04,0.40"
                /*
                [
                    "2026-01-20",
                    "11.12",
                    "11.16",
                    "11.20",
                    "11.11",
                    "772276.00",
                    {},
                    "0.40",
                    "86169.42",
                    ""
                ]
                 */
            return "" + objects.get(0).toString() // date
                      + "," + objects.get(1).toString() // open
                      + "," + objects.get(2).toString() // last
                      + "," + objects.get(3).toString() // high
                      + "," + objects.get(4).toString() // low
                      + "," + objects.get(5).toString() // volume
                      + "," + Double.parseDouble(objects.get(8).toString()) * 10000 // amount
                      + "," + "zhen_fu_ratio" // 振幅=(当天的high-当天的low)/昨天的last*100
                      + "," + "zhang_fu_ratio" // 涨幅=(当天的last-昨天的last)/昨天的last*100
                      + "," + "zhang_fu_zhi" // 涨幅值=当天的last-昨天的last
                      + "," + objects.get(7).toString() // 换手
                    ;
        }
    }
}

