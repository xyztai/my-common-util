package net.my.controller;

import io.swagger.annotations.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.AgQueryETFMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import net.my.pojo.SpecialCarePoJo;
import net.my.pojo.SpecialCarePoJo2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * etf-根据不同的逻辑来查询数据特征点
 */
@RestController
@RequestMapping("/ag-eastmoney-etf")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgQueryETFController {

    // demo: "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600276&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61";
    // fqt=1 表示前复权
    public static final String EASTMONEY_URL_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    public static final String EASTMONEY_URL_BEGIN_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=%s&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    @Autowired
    private AgQueryETFMapper agQueryETFMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    // 1、根据最近一年的数据，判断今天能否进入 TOP10
    private static final String KEY_1 = "etf#" + "special-care-days-eastmoney-365";
    // 2、根据 TOP10 查看最近60天的情况
    private static final String KEY_2 = "etf#" + "special-care-days-eastmoney-30";
    // 3、找到成交量放大2倍及以上的stock
    private static final String KEY_3 = "etf#" + "queryEastmoneyVolSuddenlyRised";
    // 5、queryEtf9ZhuanS
    private static final String KEY_5 = "etf#" + "queryEtf9ZhuanS";
    // 6、queryEtf9ZhuanB
    private static final String KEY_6 = "etf#" + "queryEtf9ZhuanB";
    // 10、近3个月的，成交量暴涨10倍的
    private static final String KEY_10 = "etf#" + "queryEtfLastest90Days";
    // 11、直接看 investEtfChgTop3 数据，只看前三
    private static final String KEY_11 = "etf#" + "investEtfChgTop3";
    // 12、2025后的历史数据 直接看 investEtfChgTop3History 数据，只看前三
    private static final String KEY_12 = "etf#" + "investEtfChgTop3History";

    /**
     * 1、根据最近一年的数据，判断今天能否进入 TOP10
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-1-top10")
    public BaseResponse queryEastmoneyToday() {
        log.info("queryEastmoneyToday");
        String key = KEY_1;
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfEastmoneyToday();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

    /**
     * 2、根据 TOP10 查看最近60天的情况
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-60-top10")
    public BaseResponse queryEastmoneyLast60() {
        log.info("specialCareDaysEastmoney");
        String key = KEY_2;
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfEastmoneyLast60();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

    /**
     * 3、找到成交量放大2倍及以上的stock
     * @return
     */
    @GetMapping("/volumn-suddenly-rised")
    public BaseResponse queryEastmoneyVolSuddenlyRised() {
        log.info("queryEastmoneyVolSuddenlyRised");
        String key = KEY_3;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfEastmoneyVolSuddenlyRised();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

    /**
     * 10、近3个月的，成交量暴涨10倍的
     * @return
     */
    @GetMapping("/etf-90-days")
    public BaseResponse queryEtfLastest90Days() {
        log.info("queryEtfLastest90Days");
        String key = KEY_10;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfLastest90Days();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

    /**
     * 99、查询每个stock的最近的数据
     * @return
     */
    @GetMapping("/eastmoney-latest-info")
    public BaseResponse queryEastmoneyLatestInfo() {
        log.info("queryEastmoneyLatestInfo");
        String key = "etf#" + "queryEastmoneyLatestInfo";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfEastmoneyLatestInfo();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }


    /**
     * 11、1、当天前三；2、当天chg>2%；3、相较于前一天，成交量放量25%~100%；4、前一天的chg<2%；5、前一天不在前三；
     * @return
     */
    @GetMapping("/etf-chg-top3")
    public BaseResponse queryEtfChgTop3() {
        log.info("queryEtfChgTop3");
        String key = KEY_11;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfChgTop3();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

    /**
     * 12、1、当天前三；2、当天chg>2%；3、相较于前一天，成交量放量25%~100%；4、前一天的chg<2%；5、前一天不在前三；
     * @return
     */
    @GetMapping("/etf-chg-top3-history")
    public BaseResponse investEtfChgTop3History() {
        log.info("investEtfChgTop3History");
        String key = KEY_12;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryETFMapper.queryEtfChgTop3History();
        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
                .filter(f -> !f.getStockCode().startsWith("688")
                        && !f.getStockCode().startsWith("689")
                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }









    /**
     * 计算expma的方法，先不删除
      */
/*
    private void updateExpma() {
        List<HsStockPoJo> etfList = agEastmoneyEtfMapper.getEtfList();
        if(CollectionUtils.isEmpty(etfList)) {
            return;
        }

        // 直接全量查出来
        List<EastmoneyNode> needUpdateExpmasAll = agEastmoneyEtfMapper.getEtfAllNeedUpdateEastMoneyNodes();
        if(CollectionUtils.isEmpty(needUpdateExpmasAll)) {
            log.info("needUpdateExpmasAll empty, return directly");
            return;
        }
        // 全量查出数据
        List<EastmoneyNode> allMaxEastMoneyNodeHasExpma = agEastmoneyEtfMapper.getEtfAllMaxEastMoneyNodeHasExpma();

        List<String> zqdms = new ArrayList<>();
        etfList.forEach(po -> zqdms.add(0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode()));

        // 1、计算非99999的数据
        for(String zqdm : zqdms) {
//            String zqdm = "0.000001";
//            List<EastmoneyNode> needUpdateExpmas = agEastmoneyEtfMapper.getEtfEastMoneyNodes(zqdm);
            List<EastmoneyNode> needUpdateExpmas = needUpdateExpmasAll.stream()
                    .filter(f -> zqdm.equals(f.getStockCode()) && !f.getDate().startsWith("99999")).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(needUpdateExpmas)) {
                log.info("zqdm={}, 不存在需要处理的非99999的数据");
                continue;
            }

            needUpdateExpmas = needUpdateExpmas.stream().sorted(Comparator.comparing(EastmoneyNode::getDate)).collect(Collectors.toList());
            log.info("needUpdateExpmas={}", JSON.toJSONString(needUpdateExpmas));
//            EastmoneyNode existsNode = agEastmoneyEtfMapper.getEtfMaxEastMoneyNodeHasExpma(zqdm);
            EastmoneyNode existsNode = allMaxEastMoneyNodeHasExpma.stream().filter(f -> zqdm.equals(f.getStockCode())).findAny().orElse(null);

            if(!CollectionUtils.isEmpty(needUpdateExpmas)) {
                List<EastmoneyNode> toUpdateItems = new ArrayList<>();
                for(int i = 0; i < needUpdateExpmas.size(); i++) {
                    EastmoneyNode currNode = needUpdateExpmas.get(i);
                    log.info("existsNode={}", JSON.toJSONString(existsNode));
                    log.info("currNode={}", JSON.toJSONString(currNode));
                    if(currNode.getDate().startsWith("99999")) {
                        log.info("currNode.getDate()={}, skip", currNode.getDate());
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
//                    agEastmoneyEtfMapper.updateEtfExpmaEastmoney(currNode);
                    toUpdateItems.add(currNode);
                }

                if(!CollectionUtils.isEmpty(toUpdateItems)) {
                    log.info("toUpdateItems={}", JSON.toJSON(toUpdateItems));
                    agEastmoneyEtfMapper.batchUpdateEtfExpmaEastmoney(toUpdateItems);
                }
            }
        }

        // 2、计算99999的数据
        allMaxEastMoneyNodeHasExpma = agEastmoneyEtfMapper.getEtfAllMaxEastMoneyNodeHasExpma();
        for(String zqdm : zqdms) {
//            String zqdm = "0.000001";
//            List<EastmoneyNode> needUpdateExpmas = agEastmoneyEtfMapper.getEtfEastMoneyNodes(zqdm);
            List<EastmoneyNode> needUpdateExpmas = needUpdateExpmasAll.stream()
                    .filter(f -> zqdm.equals(f.getStockCode()) && f.getDate().startsWith("99999")).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(needUpdateExpmas)) {
                log.info("zqdm={}, 不存在需要处理的99999的数据");
                continue;
            }

            needUpdateExpmas = needUpdateExpmas.stream().sorted(Comparator.comparing(EastmoneyNode::getDate)).collect(Collectors.toList());
            log.info("needUpdateExpmas={}", JSON.toJSONString(needUpdateExpmas));
//            EastmoneyNode existsNode = agEastmoneyEtfMapper.getEtfMaxEastMoneyNodeHasExpma(zqdm);
            EastmoneyNode existsNode = allMaxEastMoneyNodeHasExpma.stream().filter(f -> zqdm.equals(f.getStockCode())).findAny().orElse(null);

            if(!CollectionUtils.isEmpty(needUpdateExpmas)) {
                List<EastmoneyNode> toUpdateItems = new ArrayList<>();

//                existsNode = agEastmoneyEtfMapper.getEtfMaxEastMoneyNodeHasExpma(zqdm);
                if(existsNode != null) {
                    for(int i = 0; i < needUpdateExpmas.size(); i++) {
                        EastmoneyNode currNode = needUpdateExpmas.get(i);
                        currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
                        currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
                        log.info("updateExpmaEastmoney currNode={}", JSON.toJSON(currNode));
//                            agEastmoneyEtfMapper.updateEtfExpmaEastmoney(currNode);
                        toUpdateItems.add(currNode);
                    }
                }

                if(!CollectionUtils.isEmpty(toUpdateItems)) {
                    log.info("toUpdateItems={}", JSON.toJSON(toUpdateItems));
                    agEastmoneyEtfMapper.batchUpdateEtfExpmaEastmoney(toUpdateItems);
                }
            }
        }
    }

    private double calcExpma(double step, double lastValue, double cp) {
        return (cp - lastValue) * 2.0 / (step + 1) + lastValue;
    }
*/


    @Data
    public class EtfEastmoneyRes {
        private EtfEastmoneyPOJO data;
    }

    @Data
    public static class EtfEastmoneyPOJO {
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

