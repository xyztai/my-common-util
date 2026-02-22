package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.config.ScheduledTasks;
import net.my.mapper.AgEastmoneyStockMapper;
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
@RequestMapping("/ag-eastmoney-stock")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyStockController {

    // demo: "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600276&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61";
    // fqt=1 表示前复权
    public static final String EASTMONEY_URL_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    public static final String EASTMONEY_URL_BEGIN_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=%s&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private AgEastmoneyStockMapper agEastmoneyStockMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AgEastmoneyWinRatioMapper agEastmoneyWinRatioMapper;

    @Autowired
    private AgNewQQ300Controller agNewQQ300Controller;

    @Autowired
    private AgNewXueqiuController agNewXueqiuController;

    @Autowired
    private AgNewSohuController agNewSohuController;

    @Autowired
    private AgNewSinaController agNewSinaController;



//    /**
//     * 1、根据 t_eastmoney_node_buy 表获取最近的300条记录
//     * @return
//     */
//    @GetMapping("/special-care-days-eastmoney")
//    public BaseResponse specialCareDaysEastmoney() {
//        log.info("specialCareDaysEastmoney");
//
//        String key = "special-care-days-eastmoney";
//        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
//        if(res != null) {
//            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
//            return RestGeneralResponse.of(res);
//        }
//
//        List<SpecialCarePoJo> buyDataFromEastmoneys = agEastmoneyStockMapper.queryEastmoneyExistedBuyData();
//        buyDataFromEastmoneys = buyDataFromEastmoneys.stream().filter(f -> !f.getStockCode().startsWith("688") && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
//
//        myCaffeineCache.put(key, buyDataFromEastmoneys);
//        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
//        return RestGeneralResponse.of(buyDataFromEastmoneys);
//    }

    /**
     * 1、根据最近一年的数据，判断今天能否进入TOP3
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-1-top3")
    public BaseResponse queryEastmoneyToday() {
        log.info("queryEastmoneyToday");
        String key = "stock#" + "special-care-days-eastmoney-365";
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryEastmoneyToday();
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
     * 2、根据top3查看最近30天的情况
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-30-top3")
    public BaseResponse queryEastmoneyLast30() {
        log.info("specialCareDaysEastmoney");
        String key = "stock#" + "special-care-days-eastmoney-30";
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryEastmoneyLast30();
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
     * 3、找到成交量放大3倍及以上的stock
     * @return
     */
    @GetMapping("/volumn-suddenly-rised")
    public BaseResponse queryEastmoneyVolSuddenlyRised() {
        log.info("queryEastmoneyVolSuddenlyRised");
        String key = "stock#" + "queryEastmoneyVolSuddenlyRised";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryEastmoneyVolSuddenlyRised();
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
     * 5、query9ZhuanB
     * @return
     */
    @GetMapping("/query9ZhuanB")
    public BaseResponse query9ZhuanB() {
        log.info("query9ZhuanB");
        String key = "stock#" + "query9ZhuanB";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.query9ZhuanB();
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
     * 6、query9ZhuanS
     * @return
     */
    @GetMapping("/query9ZhuanS")
    public BaseResponse query9ZhuanS() {
        log.info("query9ZhuanS");
        String key = "stock#" + "query9ZhuanS";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.query9ZhuanS();
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
//        String key = "stock#" + "queryWinRatios";
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
        String key = "stock#" + "queryEastmoneyLatestInfo";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryEastmoneyLatestInfo();
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
     * 9、方便截屏
     * @return
     */
    @GetMapping("/query9ZhuanCodes")
    public BaseResponse query9ZhuanCodes() {
        String key1 = "stock#" + "query9ZhuanS";
        List<SpecialCarePoJo2> res1 = (List<SpecialCarePoJo2>) myCaffeineCache.get(key1);
        String key2 = "stock#" + "query9ZhuanB";
        List<SpecialCarePoJo2> res2 = (List<SpecialCarePoJo2>) myCaffeineCache.get(key2);
        String key5 = "stock#" + "queryEastmoneyVolSuddenlyRised";
        List<SpecialCarePoJo2> res5 = (List<SpecialCarePoJo2>) myCaffeineCache.get(key5);

        if(CollectionUtils.isEmpty(res1)
                && CollectionUtils.isEmpty(res2)
                && CollectionUtils.isEmpty(res5)) {
            return RestGeneralResponse.OK;
        }

        List<SpecialCarePoJo2> res3 = new ArrayList<>();
        if(!CollectionUtils.isEmpty(res1)){
            List<String> dates = res1.stream().map(SpecialCarePoJo2::getDate).sorted(Comparator.reverseOrder()).distinct().collect(Collectors.toList());

            List<String> targetDates = dates;
            if(dates.size() > 1) {
                targetDates = dates.subList(0, 1);
            }

            List<String> finalTargetDates = targetDates;
            List<SpecialCarePoJo2> codes = res1.stream()
                    .filter(f -> finalTargetDates.contains(f.getDate()))
                    .collect(Collectors.toList());
            for(int i = 0; i < codes.size(); i++) {
                SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
//                tmp.setDate(codes.get(i).getDate());
                tmp.setStockCode(codes.get(i).getStockCode().substring(0, 6));
//                tmp.setLast("No." + i);
//                tmp.setRatioB(codes.get(i).getRatioB());
                tmp.setRatioB("S69");
                res3.add(tmp);
            }
        }

        if(!CollectionUtils.isEmpty(res2)){
            List<String> dates = res2.stream().map(SpecialCarePoJo2::getDate).sorted(Comparator.reverseOrder()).distinct().collect(Collectors.toList());

            List<String> targetDates = dates;
            if(dates.size() > 5) {
                targetDates = dates.subList(0, 5);
            }

            List<String> finalTargetDates = targetDates;
            List<SpecialCarePoJo2> codes = res2.stream()
                    .filter(f -> finalTargetDates.contains(f.getDate()) && f.getRatioB().startsWith("B_09"))
                    .collect(Collectors.toList());
            for(int i = 0; i < codes.size(); i++) {
                SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
//                tmp.setDate(codes.get(i).getDate());
                tmp.setStockCode(codes.get(i).getStockCode().substring(0, 6));
//                tmp.setLast("No." + i);
                tmp.setRatioB("B9");
                res3.add(tmp);
            }
        }

        if(!CollectionUtils.isEmpty(res5)){
            List<SpecialCarePoJo2> codes = res5.stream()
                    .filter(f -> f.getRatioB().startsWith("慎重"))
                    .collect(Collectors.toList());
            for(int i = 0; i < codes.size(); i++) {
                SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
//                tmp.setDate(codes.get(i).getDate());
                tmp.setStockCode(codes.get(i).getStockCode().substring(0, 6));
//                tmp.setLast("No." + i);
                tmp.setRatioB("vol*3");
                res3.add(tmp);
            }
        }

        String key6 = "stock#" + "queryLastest9Zhuan";
        List<SpecialCarePoJo2> res6 = (List<SpecialCarePoJo2>) myCaffeineCache.get(key6);
        if(CollectionUtils.isEmpty(res6)) {
            res6 = agEastmoneyStockMapper.queryLastest9Zhuan();
            myCaffeineCache.put(key6, res6);
        }
        if(!CollectionUtils.isEmpty(res6)){
            List<SpecialCarePoJo2> codes = res6;
            for(int i = 0; i < codes.size(); i++) {
                SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
//                tmp.setDate(codes.get(i).getDate());
                tmp.setStockCode(codes.get(i).getStockCode().substring(0, 6));
//                tmp.setLast("No." + i);
                tmp.setRatioB("近5_9v");
                res3.add(0, tmp);
            }
        }

        List<SpecialCarePoJo2> res = new ArrayList<>();
        List<String> stockExists = new ArrayList<>();
        if(!CollectionUtils.isEmpty(res3)) {
            for(SpecialCarePoJo2 jo2 : res3) {
                if(stockExists.contains(jo2.getStockCode())) {
                    continue;
                }
                res.add(jo2);
                stockExists.add(jo2.getStockCode());
            }
        }

//        int cnt = codes.size()/4;
//        for(int i = 0; i < cnt + 1; i++) {
//            SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
//            if(i * 4 < codes.size()) {
//                tmp.setDate(codes.get(i * 4));
//            } else {
//                continue;
//            }
//
//            if(i * 4 + 1 < codes.size()) {
//                tmp.setStockCode(codes.get(i * 4 + 1));
//            }
//            else {
//                res2.add(tmp);
//                continue;
//            }
//
//            if(i * 4 + 2 < codes.size()) {
//                tmp.setLast(codes.get(i * 4 + 2));
//            }
//            else {
//                res2.add(tmp);
//                continue;
//            }
//
//            if(i * 4 + 3 < codes.size()) {
//                tmp.setRatioB(codes.get(i * 4 + 3));
//            }
//            else {
//                res2.add(tmp);
//                continue;
//            }
//            res2.add(tmp);
//        }
        return RestGeneralResponse.of(res);
    }

    /**
     * 10、近3个月的，成交量暴涨9倍的
     * @return
     */
    @GetMapping("/eastmoney-9-vol-in-90-days")
    public BaseResponse query9VolInLastest90Days() {
        log.info("query9VolInLastest90Days");
        String key = "stock#" + "query9VolInLastest90Days";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.query9VolInLastest90Days();
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
     * 11、查询 60 日均
     * @return
     */
    @GetMapping("/eastmoney-avg-60")
    public BaseResponse queryAvg60() {
        log.info("queryAvg60");
        String key = "stock#" + "queryAvg60";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryAvg60();
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
        if(ScheduledTasks.taskState != 0) {
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
        List<HsStockPoJo> hs300List = agEastmoneyStockMapper.getHs300List();

        if(CollectionUtils.isEmpty(hs300List)) {
            return BaseResponse.OK;
        }

        Map<String, String> hs300Map = new LinkedHashMap<>();
        for(HsStockPoJo po : hs300List) {
            String key = "3_" + po.getStockName() + "-" + po.getStockCode();
            String value = 0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode();
            hs300Map.put(key, value);
        }

        // 先sina数据
        if(CollectionUtils.isEmpty(eastmoneyNodeList)) {
            Map<String, String> sinaMap = agNewSinaController.getQQResReplaceEastmoney(1);
            if (!CollectionUtils.isEmpty(sinaMap)) {
                for (Map.Entry<String, String> entry : sinaMap.entrySet()) {
                    String item = entry.getValue();
                    String[] xxs = item.split(",");
                    eastmoneyNodeList.add(EastmoneyNode.builder().date(xxs[0]).stockCode(entry.getKey()).infoRaw(item).build());
                }
            }
        }

        // 后sohu数据
        if(CollectionUtils.isEmpty(eastmoneyNodeList)) {
            Map<String, String> soHuMap = agNewSohuController.getQQResReplaceEastmoney(1);
            if(!CollectionUtils.isEmpty(soHuMap)) {
                for(Map.Entry<String, String> entry : soHuMap.entrySet()) {
                    String item = entry.getValue();
                    String[] xxs = item.split(",");
                    eastmoneyNodeList.add(EastmoneyNode.builder().date(xxs[0]).stockCode(entry.getKey()).infoRaw(item).build());
                }
            }
        }

        boolean useEastmoney = true;
        boolean useQq = false;
        boolean useXueqiu = false;
        boolean useEastmoneyStop = false;
        boolean useQqStop = false;
        boolean useXueqiuStop = false;
        int zqNo = 1;
        for(Map.Entry<String, String> entry : hs300Map.entrySet()) {
            if(!CollectionUtils.isEmpty(eastmoneyNodeList)) {
                log.info("已经由sina/sohu获得数据, eastmoneyNodeList size={}", eastmoneyNodeList.size());
                String maxDate = agNewSohuController.getMaxDateFromStock();
                eastmoneyNodeList = eastmoneyNodeList.stream()
                        .filter(f -> f.getDate().compareTo(maxDate) > 0)
                        .collect(Collectors.toList());
                log.info("已经由sina/sohu获得数据, 待插入数据 eastmoneyNodeList size={}", eastmoneyNodeList.size());
                break;
            }

            String zqdm = entry.getValue();
            log.info("getHistoryData No.{}, stock:{}", zqNo++, zqdm);
//            if(!zqdm.equals("0.000001")) {
//                continue;
//            }
            
            String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);

            EastmoneyNode existsNode = agEastmoneyStockMapper.getMaxEastMoneyNode(zqdm);
            if(existsNode != null) {
                url = String.format(EASTMONEY_URL_BEGIN_FORMAT_QFQ, zqdm, existsNode.getDate().replaceAll("-", ""));
            }

            log.info("url: {}, zqdm: {}", url, zqdm);
            String res = "";
            List<String> resFromQQ = new ArrayList<>();
            for(int i = 0; i < 3; i++) {
                if(useEastmoney && !useEastmoneyStop) {
                    log.info("useWay=useEastmoney");
                    try {
                        int sleepTime = 750 + new Random().nextInt(500) - 250;
                        log.info("sleepTime={}", sleepTime);
                        Thread.sleep(sleepTime);
                        log.info("try num={}, stockCode={}, url={}", i, entry.getKey(), url);
                        res = restTemplate.getForObject(url, String.class);
                        if(!StringUtils.isEmpty(res)) {
                            if(!useQqStop) {
                                useQq = true;
                                useXueqiu = false;
                                useEastmoney = false;
                            } else if(!useXueqiuStop) {
                                useXueqiu = true;
                                useEastmoney = false;
                            } else {
                                Thread.sleep(sleepTime);
                            }
                            break;
                        }
                    } catch (Exception ex) {
                        useEastmoneyStop = true;
                        if(!useQqStop) {
                            useQq = true;
                        } else if(!useXueqiuStop) {
                            useXueqiu = true;
                        }
                        log.error("eastmoney error", ex);
                    }
                }

                if(useQq && !useQqStop) {
                    log.info("useWay=useQq");
                    try {
                        int sleepTime = 750 + new Random().nextInt(500) - 250;
                        log.info("sleepTime={}", sleepTime);
                        Thread.sleep(sleepTime);
                        resFromQQ = agNewQQ300Controller.getQQResReplaceEastmoney(zqdm.replace("0.", "sz").replace("1.", "sh"), 30);
                        if(!CollectionUtils.isEmpty(resFromQQ)) {
                            log.info("useQq getQQResReplaceEastmoney res {}:{}", zqdm, JSON.toJSON(resFromQQ));
                            if(!useXueqiuStop) {
                                useXueqiu = true;
                                useEastmoney = false;
                                useQq = false;
                            } else if(!useEastmoneyStop) {
                                useEastmoney = true;
                                useQq = false;
                            } else {
                                Thread.sleep(sleepTime);
                            }
                            break;
                        }
                    } catch (Exception ex) {
                        useQqStop = true;
                        if(!useXueqiuStop) {
                            useXueqiu = true;
                        } else if(!useEastmoneyStop) {
                            useEastmoney = true;
                        }
                        log.error("useQq getQQResReplaceEastmoney error", ex);
                    }
                }

                if(useXueqiu && !useXueqiuStop) {
                    log.info("useWay=useXueqiu");
                    try {
                        int sleepTime = 750 + new Random().nextInt(500) - 250;
                        log.info("sleepTime={}", sleepTime);
                        Thread.sleep(sleepTime);
                        resFromQQ = agNewXueqiuController.getQQResReplaceEastmoney(zqdm.replace("0.", "SZ").replace("1.", "SH"), 30);
                        if(!CollectionUtils.isEmpty(resFromQQ)) {
                            log.info("useXueqiu getQQResReplaceEastmoney res {}:{}", zqdm, JSON.toJSON(resFromQQ));
                            if(!useEastmoneyStop) {
                                useEastmoney = true;
                                useQq = false;
                                useXueqiu = false;
                            } else if(!useQqStop) {
                                useQq = true;
                                useXueqiu = false;
                            } else {
                                Thread.sleep(sleepTime);
                            }
                            break;
                        }
                    } catch (Exception ex) {
                        useXueqiuStop = true;
                        if(!useEastmoneyStop) {
                            useEastmoney = true;
                        } else if(!useQqStop) {
                            useQq = true;
                        }
                        log.error("useXueqiu getQQResReplaceEastmoney error", ex);
                    }
                }

                if(!StringUtils.isEmpty(res) || !CollectionUtils.isEmpty(resFromQQ)) {
                    break;
                }
            }
            if(StringUtils.isEmpty(res) && CollectionUtils.isEmpty(resFromQQ)) {
                continue;
            }

            try {
                List<String> klines = new ArrayList<>();
                if(!StringUtils.isEmpty(res)) {
                    log.info("res={}", res);
                    EastmoneyStockRes eastmoneyRes = JSON.parseObject(res, EastmoneyStockRes.class);
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
            agEastmoneyStockMapper.saveEastMoneyDatas(tmpNodes);
            startNum += stepNum;
        }

        log.info("阶段1-非99999数据-开始更新基础字段");
        // 更新基础字段
        agEastmoneyStockMapper.updateEastMoneyDatas();

        // 更新expma字段
        log.info("阶段1-非99999数据-开始更新expma字段 start");
        updateExpma();
        log.info("阶段1-非99999数据-开始更新expma字段 end");

        // 删除 预期数据
        log.info("阶段2-99999数据-deleteExpect99999");
        agEastmoneyStockMapper.deleteExpect99999();
        // 开始插入 预期数据
        log.info("阶段2-99999数据-insertExpect99999");
        agEastmoneyStockMapper.insertExpect99999();

        // 更新基础字段
        log.info("阶段2-99999数据-开始更新基础字段");
        agEastmoneyStockMapper.updateEastMoneyDatas();

        // 更新expma字段
        log.info("阶段2-99999数据-开始更新expma字段 start");
        updateExpma();
        log.info("阶段2-99999数据-开始更新expma字段 end");
//        log.info("删除 delEastMoneyBuy99999");
//        agEastmoneyStockMapper.delEastMoneyBuy99999();
//        // 更新buy表
//        List<String> needCalcBuys = agEastmoneyStockMapper.getNeedCalcDates("2025-06-01", "20");
//        if(!CollectionUtils.isEmpty(needCalcBuys)) {
//            for(String currDate : needCalcBuys) {
//                log.info("calcBuy currDate={}", currDate);
//                agEastmoneyStockMapper.saveEastmoneyNodeBuys(currDate);
//            }
//        }

//        qqNodeMap.values().forEach(qq -> dataCalcMapper.saveQqNode(qq));
        return RestGeneralResponse.of(eastmoneyNodeMap);
    }

    private void updateExpma() {
        List<HsStockPoJo> hs300List = agEastmoneyStockMapper.getHs300List();
        if(CollectionUtils.isEmpty(hs300List)) {
            return;
        }

        // 直接全量查出来
        List<EastmoneyNode> needUpdateExpmasAll = agEastmoneyStockMapper.getAllNeedUpdateEastMoneyNodes();
        if(CollectionUtils.isEmpty(needUpdateExpmasAll)) {
            log.info("needUpdateExpmasAll empty, return directly");
            return;
        }
        // 全量查出数据
        List<EastmoneyNode> allMaxEastMoneyNodeHasExpma = agEastmoneyStockMapper.getAllMaxEastMoneyNodeHasExpma();

        List<String> zqdms = new ArrayList<>();
        hs300List.forEach(po -> zqdms.add(0 == po.getStockType() ? "0." + po.getStockCode() : "1." + po.getStockCode()));

        // 1、计算非99999的数据
        for(String zqdm : zqdms) {
//            String zqdm = "0.000001";
//            List<EastmoneyNode> needUpdateExpmas = agEastmoneyStockMapper.getEastMoneyNodes(zqdm);
            List<EastmoneyNode> needUpdateExpmas = needUpdateExpmasAll.stream()
                    .filter(f -> zqdm.equals(f.getStockCode()) && !f.getDate().startsWith("99999")).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(needUpdateExpmas)) {
                log.info("zqdm={}, 不存在需要处理的非99999的数据");
                continue;
            }

            needUpdateExpmas = needUpdateExpmas.stream().sorted(Comparator.comparing(EastmoneyNode::getDate)).collect(Collectors.toList());
            log.info("needUpdateExpmas={}", JSON.toJSONString(needUpdateExpmas));
//            EastmoneyNode existsNode = agEastmoneyStockMapper.getMaxEastMoneyNodeHasExpma(zqdm);
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
//                    agEastmoneyStockMapper.updateExpmaEastmoney(currNode);
                    toUpdateItems.add(currNode);
                }

                if(!CollectionUtils.isEmpty(toUpdateItems)) {
                    log.info("toUpdateItems={}", JSON.toJSON(toUpdateItems));
                    agEastmoneyStockMapper.batchUpdateExpmaEastmoney(toUpdateItems);
                }
            }
        }

        // 2、计算99999的数据
        allMaxEastMoneyNodeHasExpma = agEastmoneyStockMapper.getAllMaxEastMoneyNodeHasExpma();
        for(String zqdm : zqdms) {
//            String zqdm = "0.000001";
//            List<EastmoneyNode> needUpdateExpmas = agEastmoneyStockMapper.getEastMoneyNodes(zqdm);
            List<EastmoneyNode> needUpdateExpmas = needUpdateExpmasAll.stream()
                    .filter(f -> zqdm.equals(f.getStockCode()) && f.getDate().startsWith("99999")).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(needUpdateExpmas)) {
                log.info("zqdm={}, 不存在需要处理的99999的数据");
                continue;
            }

            needUpdateExpmas = needUpdateExpmas.stream().sorted(Comparator.comparing(EastmoneyNode::getDate)).collect(Collectors.toList());
            log.info("needUpdateExpmas={}", JSON.toJSONString(needUpdateExpmas));
//            EastmoneyNode existsNode = agEastmoneyStockMapper.getMaxEastMoneyNodeHasExpma(zqdm);
            EastmoneyNode existsNode = allMaxEastMoneyNodeHasExpma.stream().filter(f -> zqdm.equals(f.getStockCode())).findAny().orElse(null);

            if(!CollectionUtils.isEmpty(needUpdateExpmas)) {
                List<EastmoneyNode> toUpdateItems = new ArrayList<>();

//                existsNode = agEastmoneyStockMapper.getMaxEastMoneyNodeHasExpma(zqdm);
                if(existsNode != null) {
                    for(int i = 0; i < needUpdateExpmas.size(); i++) {
                        EastmoneyNode currNode = needUpdateExpmas.get(i);
                        currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
                        currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
                        log.info("updateExpmaEastmoney currNode={}", JSON.toJSON(currNode));
//                            agEastmoneyStockMapper.updateExpmaEastmoney(currNode);
                        toUpdateItems.add(currNode);
                    }
                }

                if(!CollectionUtils.isEmpty(toUpdateItems)) {
                    log.info("toUpdateItems={}", JSON.toJSON(toUpdateItems));
                    agEastmoneyStockMapper.batchUpdateExpmaEastmoney(toUpdateItems);
                }
            }
        }
    }


    private double calcExpma(double step, double lastValue, double cp) {
        return (cp - lastValue) * 2.0 / (step + 1) + lastValue;
    }


    @Data
    public class EastmoneyStockRes {
        private EastmoneyStockPOJO data;
    }

    @Data
    public static class EastmoneyStockPOJO {
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

