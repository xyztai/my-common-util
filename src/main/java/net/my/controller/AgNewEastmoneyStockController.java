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

    // 1、根据最近一年的数据，判断今天能否进入TOP3， 日期是以9999开头
    private static final String KEY_1 = "stock#" + "special-care-days-eastmoney-365-top3";
    // 2、根据top3查看最近30天的情况， 日期是以9999开头
    private static final String KEY_2 = "stock#" + "special-care-days-eastmoney-30";
    // 3、找到成交量放大3倍及以上的stock
    private static final String KEY_3 = "stock#" + "queryEastmoneyVolSuddenlyRisedTriple";
    // 5、query9ZhuanS
    private static final String KEY_5 = "stock#" + "query9ZhuanS";
    // 6、query9ZhuanB
    private static final String KEY_6 = "stock#" + "query9ZhuanB";
    // 10、近3个月的，成交量暴涨9倍的
    private static final String KEY_10 =  "stock#" + "query9VolInLastest90Days";
    // 11、查询 60 日均
    private static final String KEY_11 = "stock#" + "queryAvg60";
    // 12、查询最近的冲顶
    private static final String KEY_12 = "stock#" + "queryLatestRiseLimit";
    // 13、查询最近一个月的大波动
    private static final String KEY_13 = "stock#" + "queryBigSwing";
    // 14、查询最近一个月的大波动且最低Vol
    private static final String KEY_14 = "stock#" + "queryBigSwingAndLowestVol";
    // 15、查询多头排列的票
    private static final String KEY_15 = "stock#" + "queryDuoTou";
    // 16、查询多头排列的票（ma多头）
    private static final String KEY_16 = "stock#" + "queryDuoTouMA";
    // 17、5连up
    private static final String KEY_17 = "stock#" + "queryUp5Lian";
    // 18、25年后上涨2倍，且存在短期快速上涨
    private static final String KEY_18 = "stock#" + "queryOnlyThem";

    /**
     * 0、方便截屏
     * @return
     */
    @GetMapping("/easy-snapshot")
    public BaseResponse easySnapshot() {
        Set<String> stockExists = new HashSet<>();
        List<SpecialCarePoJo2> res = new ArrayList<>();
        String limitDate = agEastmoneyStockMapper.getLimitDate();

        Map<String, List<SpecialCarePoJo2>> tmpMap = new HashMap<>();

        // 0、先查询9转+9vol
        String key0 = "stock#" + "queryLastest9Zhuan";
        List<SpecialCarePoJo2> res0 = (List<SpecialCarePoJo2>) myCaffeineCache.get(key0);
        if(CollectionUtils.isEmpty(res0)) {
            res0 = agEastmoneyStockMapper.queryLastest9Zhuan9Vol();
            myCaffeineCache.put(key0, res0);
        }
        if(!CollectionUtils.isEmpty(res0)){
            res0.forEach(f -> f.setRatioB("*近5v9"));
            res0 = res0.stream()
                    .filter(f -> f.getDate().compareTo(limitDate) >= 0)
                    .sorted(Comparator.comparing(SpecialCarePoJo2::getDate).reversed())
                    .collect(Collectors.toList());

            List<SpecialCarePoJo2> tmpList = new ArrayList<>();
            for(SpecialCarePoJo2 poJo2 : res0) {
                SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
                tmp.setDate(poJo2.getDate().substring(0,10));
                tmp.setLast("");
                tmp.setRatioB(poJo2.getRatioB());
                tmp.setStockCode(poJo2.getStockCode().substring(0, 6));
                tmpList.add(tmp);
//                if(!stockExists.contains(tmp.getStockCode())) {
//                    res.add(tmp);
//                    stockExists.add(tmp.getStockCode());
//                }
            }
            tmpMap.put(key0, tmpList);
        }

        for(String kk : Arrays.asList(KEY_1, KEY_11, KEY_3, KEY_5, KEY_6, KEY_10)) {
            res0 = (List<SpecialCarePoJo2>) myCaffeineCache.get(kk);
            if(!CollectionUtils.isEmpty(res0)){
                res0 = res0.stream()
                        .filter(f -> f.getDate().compareTo(limitDate) >= 0)
                        .sorted(Comparator.comparing(SpecialCarePoJo2::getDate).reversed())
                        .collect(Collectors.toList());

                List<SpecialCarePoJo2> tmpList = new ArrayList<>();
                for(SpecialCarePoJo2 poJo2 : res0) {
                    SpecialCarePoJo2 tmp = new SpecialCarePoJo2();
                    tmp.setDate(poJo2.getDate().substring(0,10));
                    tmp.setLast("");
                    switch (kk) {
                        case KEY_1:
                            tmp.setRatioB("*top3");
                            break;
                        case KEY_3:
                            tmp.setRatioB("vol*3");
                            break;
                        case KEY_5:
                            tmp.setRatioB("s69");
                            break;
                        case KEY_6:
                            tmp.setRatioB("b69");
                            break;
                        case KEY_10:
                            tmp.setRatioB("vol*9");
                            break;
                        case KEY_11:
                            tmp.setRatioB("*avg");
                            break;
                    }
                    tmp.setStockCode(poJo2.getStockCode().substring(0, 6));
                    tmpList.add(tmp);
//                    if(!stockExists.contains(tmp.getStockCode())) {
//                        res.add(tmp);
//                        stockExists.add(tmp.getStockCode());
//                    }
                }
                tmpMap.put(kk, tmpList);
            }
        }

        // 这里是对所有的数据进行排序处理，优先看avg，然后看top3，再看"9转+9vol"，剩下的就按照顺序来吧
//        for(String kk : Arrays.asList(KEY_11, KEY_1, key0, KEY_3, KEY_5, KEY_6, KEY_10)) {
        for(String kk : Arrays.asList(KEY_11)) {
            List<SpecialCarePoJo2> tmpList = tmpMap.get(kk);
            if(!CollectionUtils.isEmpty(tmpList)) {
                for(SpecialCarePoJo2 tmp : tmpList) {
                    if(!stockExists.contains(tmp.getStockCode())) {
                        res.add(tmp);
                        stockExists.add(tmp.getStockCode());
                    }
                }
            }
        }


        if(!CollectionUtils.isEmpty(res)) {
            log.info("easySnapshot:{}"
                    , String.join(",", res.stream().map(SpecialCarePoJo2::getStockCode).collect(Collectors.toList())));
        }
        return RestGeneralResponse.of(res);
    }

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
     * 1、根据最近一年的数据，判断今天能否进入TOP3， 日期是以9999开头
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-365-top3")
    public BaseResponse queryEastmoneyToday() {
        log.info("queryEastmoneyToday");
        String key = KEY_1;
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
     * 2、根据top3查看最近30天的情况， 日期是以9999开头
     * @return
     */
    @GetMapping("/special-care-days-eastmoney-30-top3")
    public BaseResponse queryEastmoneyLast30() {
        log.info("specialCareDaysEastmoney");
        String key = KEY_2;
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
    @GetMapping("/volumn-suddenly-rised-tiple")
    public BaseResponse queryEastmoneyVolSuddenlyRisedTriple() {
        log.info("queryEastmoneyVolSuddenlyRisedTriple");
        String key = KEY_3;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryEastmoneyVolSuddenlyRisedTriple();
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
     * 5、query9ZhuanS
     * @return
     */
    @GetMapping("/query9ZhuanS")
    public BaseResponse query9ZhuanS() {
        log.info("query9ZhuanS");
        String key = KEY_5;
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


    /**
     * 6、query9ZhuanB
     * @return
     */
    @GetMapping("/query9ZhuanB")
    public BaseResponse query9ZhuanB() {
        log.info("query9ZhuanB");
        String key = KEY_6;
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
     * 10、近3个月的，成交量暴涨9倍的
     * @return
     */
    @GetMapping("/eastmoney-9-vol-in-90-days")
    public BaseResponse query9VolInLastest90Days() {
        log.info("query9VolInLastest90Days");
        String key = KEY_10;
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
        String key = KEY_11;
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

    /**
     * 12、查询最近的冲顶
     * * @return
     */
    @GetMapping("/eastmoney-latest-rise-limit")
    public BaseResponse queryLatestRiseLimit() {
        log.info("queryLatestRiseLimit");
        String key = KEY_12;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryLatestRiseLimit();
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
     * 13、查询最近一个月的大波动
     * * @return
     */
    @GetMapping("/eastmoney-queryBigSwing")
    public BaseResponse queryBigSwing() {
        log.info("queryBigSwing");
        String key = KEY_13;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryBigSwing();
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
     * 14、查询最近一个月的大波动且最低Vol
     * * @return
     */
    @GetMapping("/eastmoney-queryBigSwingAndLowestVol")
    public BaseResponse queryBigSwingAndLowestVol() {
        log.info("queryBigSwingAndLowestVol");
        String key = KEY_14;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryBigSwingAndLowestVol();
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
     * 15、查询多头排列的票
     * * @return
     */
    @GetMapping("/eastmoney-queryDuoTou")
    public BaseResponse queryDuoTou() {
        log.info("queryDuoTou");
        String key = KEY_15;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        // 计算MA
        genMA();
        // 计算多头数据
        genDuoTou();
        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryDuoTou();
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

    public void genMA() {
        log.info("genMA start");
        List<String> calcDates = agEastmoneyStockMapper.getCalcDatesFromMA();
        if(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("genMA calcDate={} start", calcDate);
                agEastmoneyStockMapper.genMA(calcDate);
                log.info("genMA calcDate={} end", calcDate);
            }
        }
        log.info("genMA end");
    }

    public void genDuoTou() {
        log.info("genDuoTou start");
        List<String> calcDates = agEastmoneyStockMapper.getCalcDatesFromDuoTou();
        if(!CollectionUtils.isEmpty(calcDates)) {
            for(String calcDate : calcDates) {
                log.info("genDuoTou calcDate={} start", calcDate);
                agEastmoneyStockMapper.genDuoTou(calcDate);
                log.info("genDuoTou calcDate={} end", calcDate);
            }
        }
        log.info("genDuoTou end");
    }

    /**
     * 16、查询多头排列的票（ma多头）
     * * @return
     */
    @GetMapping("/eastmoney-queryDuoTouMA")
    public BaseResponse queryDuoTouMA() {
        log.info("queryDuoTouMA");
        String key = KEY_16;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryDuoTouMA();
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
     * 17、5连up
     * * @return
     */
    @GetMapping("/eastmoney-queryUp5Lian")
    public BaseResponse queryUp5Lian() {
        log.info("queryUp5Lian");
        String key = KEY_17;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryUp5Lian();
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
     * 18、25年后上涨2倍，且存在短期快速上涨
     * * @return
     */
    @GetMapping("/eastmoney-queryOnlyThem")
    public BaseResponse queryOnlyThem() {
        log.info("queryOnlyThem");
        String key = KEY_18;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryOnlyThem();
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

