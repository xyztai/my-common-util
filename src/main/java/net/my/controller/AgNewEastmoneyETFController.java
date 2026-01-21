package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.config.ScheduledTasks;
import net.my.mapper.AgEastmoneyEtfMapper;
import net.my.mapper.AgEastmoneyWinRatioMapper;
import net.my.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/ag-eastmoney-etf")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyETFController {

    // demo: "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600276&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61";
    // fqt=1 表示前复权
    public static final String EASTMONEY_URL_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    public static final String EASTMONEY_URL_BEGIN_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=%s&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    @Autowired
    private AgEastmoneyEtfMapper agEastmoneyEtfMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private AgEastmoneyWinRatioMapper agEastmoneyWinRatioMapper;

    @Autowired
    private AgNewQQ300Controller agNewQQ300Controller;


    /**
     * 1、根据最近一年的数据，判断今天能否进入 TOP10
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-1-top10")
    public BaseResponse queryEastmoneyToday() {
        log.info("queryEastmoneyToday");
        String key = "etf#" + "special-care-days-eastmoney-365";
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyEtfMapper.queryEtfEastmoneyToday();
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
        String key = "etf#" + "special-care-days-eastmoney-30";
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyEtfMapper.queryEtfEastmoneyLast60();
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
        String key = "etf#" + "queryEastmoneyVolSuddenlyRised";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyEtfMapper.queryEtfEastmoneyVolSuddenlyRised();
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
     * 5、queryEtf9ZhuanB
     * @return
     */
    @GetMapping("/queryEtf9ZhuanB")
    public BaseResponse queryEtf9ZhuanB() {
        log.info("queryEtf9ZhuanB");
        String key = "etf#" + "queryEtf9ZhuanB";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyEtfMapper.queryEtf9ZhuanB();
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
        } else {
            String methodName = key;
            agEastmoneyEtfMapper.delEtfEastMoneyTmpCalc(methodName);
            List<EastmoneyTmpCalc> calcs = buyDataFromEastmoneys.stream().map(f -> f.toPO(methodName)).collect(Collectors.toList());
            agEastmoneyEtfMapper.saveEtfEastMoneyTmpCalc(calcs);
        }

        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }


    /**
     * 6、queryEtf9ZhuanS
     * @return
     */
    @GetMapping("/queryEtf9ZhuanS")
    public BaseResponse queryEtf9ZhuanS() {
        log.info("queryEtf9ZhuanS");
        String key = "etf#" + "queryEtf9ZhuanS";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyEtfMapper.queryEtf9ZhuanS();
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


//    /**
//     * 7、queryWinRatios
//     * @return
//     */
//    @GetMapping("/queryWinRatios")
//    public BaseResponse queryWinRatios() {
//        log.info("queryWinRatios");
//        String key = "etf#" + "queryWinRatios";
//        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
//        if(res != null) {
//            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
//            return RestGeneralResponse.of(res);
//        }
//
//        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyWinRatioMapper.queryWinRatios();
//        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
//                .filter(f -> !f.getStockCode().startsWith("688")
//                        && !f.getStockCode().startsWith("689")
//                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
//        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
//            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
//            empty.setDate("--");
//            empty.setStockCode("--");
//            empty.setRatioB("--");
//            empty.setLast("--");
//            buyDataFromEastmoneys = Arrays.asList(empty);
//        }
//
//        myCaffeineCache.put(key, buyDataFromEastmoneys);
//        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
//        return RestGeneralResponse.of(buyDataFromEastmoneys);
//    }

    /**
     * 8、查询每个stock的最近的数据
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyEtfMapper.queryEtfEastmoneyLatestInfo();
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

    @ApiOperation(value = "获取历史的cp数据", notes = "访问互联网接口获取数据")
    @GetMapping("/historyAll")
    @Transactional
    public BaseResponse getHistoryDataOuter() {
        if (ScheduledTasks.taskState != 0) {
            log.info("taskState={},放弃本次执行", ScheduledTasks.taskState);
            return BaseResponse.OK;
        }
        ScheduledTasks.taskState = 1;

        BaseResponse response = getHistoryData();
        ScheduledTasks.taskState = 0;
        return response;
    }

    @Transactional
    public BaseResponse getHistoryData() {
        List<EastmoneyNode> eastmoneyNodeList = new ArrayList<>();
        Map<String, EastmoneyNode> eastmoneyNodeMap = new LinkedHashMap<>();
        List<HsStockPoJo> etfList = agEastmoneyEtfMapper.getEtfList();

        if(CollectionUtils.isEmpty(etfList)) {
            return BaseResponse.OK;
        }

        Map<String, String> etfMap = new LinkedHashMap<>();
        for(HsStockPoJo po : etfList) {
            String key = po.getStockName() + "-" + po.getStockCode();
            String value = 0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode();
            etfMap.put(key, value);
        }


        for(Map.Entry<String, String> entry : etfMap.entrySet()) {
            String zqdm = entry.getValue();
//            if(!zqdm.equals("0.000001")) {
//                continue;
//            }
            
            String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);

            EastmoneyNode existsNode = agEastmoneyEtfMapper.getEtfMaxEastMoneyNode(zqdm);
            if(existsNode != null) {
                url = String.format(EASTMONEY_URL_BEGIN_FORMAT_QFQ, zqdm, existsNode.getDate().replaceAll("-", ""));
            }

            log.info("url: {}, zqdm: {}", url, zqdm);
            String res = "";
            List<String> resFromQQ = new ArrayList<>();
            for(int i = 0; i < 200; i++) {
                try {
                    Thread.sleep(200);
                    log.info("try num={}, stockCode={}, url={}", i, entry.getKey(), url);
                    res = restTemplate.getForObject(url, String.class);
                    if(!StringUtils.isEmpty(res)) {
                        break;
                    }
                    resFromQQ = agNewQQ300Controller.getQQResReplaceEastmoney(zqdm.replace("0.", "sz").replace("1.", "sh"), 10);
                    if(!CollectionUtils.isEmpty(resFromQQ)) {
                        log.info("{}:{}", zqdm, JSON.toJSON(resFromQQ));
                        break;
                    }
                } catch (Exception ex) {
                    ;
                }
            }
            if(StringUtils.isEmpty(res) && CollectionUtils.isEmpty(resFromQQ)) {
                continue;
            }

            try {
                List<String> klines = new ArrayList<>();
                if(!StringUtils.isEmpty(res)) {
                    log.info("res={}", res);
                    EtfEastmoneyRes eastmoneyRes = JSON.parseObject(res, EtfEastmoneyRes.class);
                    log.info("eastmoneyRes={}", JSON.toJSONString(eastmoneyRes));

//                break;

                    // saveEastMoneyDatas
                    if(eastmoneyRes == null || eastmoneyRes.getData() == null || CollectionUtils.isEmpty(eastmoneyRes.getData().getKlines())) {
                        break ;
                    }

                    klines = eastmoneyRes.getData().getKlines();
                } else {
                    klines = resFromQQ;
                }

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
            agEastmoneyEtfMapper.saveEtfEastMoneyDatas(tmpNodes);
            startNum += stepNum;
        }

        log.info("阶段1-非99999数据-开始更新基础字段");
        // 更新基础字段
        agEastmoneyEtfMapper.updateEtfEastMoneyDatas();

        // 更新expma字段
        log.info("阶段1-非99999数据-开始更新expma字段 start");
        updateExpma();
        log.info("阶段1-非99999数据-开始更新expma字段 end");

        // 删除 预期数据
        log.info("阶段2-99999数据-deleteEtfExpect99999");
        agEastmoneyEtfMapper.deleteEtfExpect99999();
        // 开始插入 预期数据
        log.info("阶段2-99999数据-insertEtfExpect99999");
        agEastmoneyEtfMapper.insertEtfExpect99999();

        // 更新基础字段
        log.info("阶段2-99999数据-开始更新基础字段");
        agEastmoneyEtfMapper.updateEtfEastMoneyDatas();

        // 更新expma字段
        log.info("阶段2-99999数据-开始更新expma字段 start");
        updateExpma();
        log.info("阶段2-99999数据-开始更新expma字段 end");

        return RestGeneralResponse.of(eastmoneyNodeMap);
    }

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

