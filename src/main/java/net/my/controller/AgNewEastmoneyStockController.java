package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.config.ScheduledTasks;
import net.my.mapper.AgCCIEastmoneyStockMapper;
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
    private AgCCIEastmoneyStockMapper agCCIEastmoneyStockMapper;

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

    // 10000、方便截图，进行数据汇总（左侧）
    private static final String KEY_10000 = "stock#" + "easy-snapshot-left";
    // 10000、方便截图，进行数据汇总（右侧）
    private static final String KEY_10001 = "stock#" + "easy-snapshot-right";

    // 101、查询出现5连跌，且当天的开盘>收盘、chg<0，可以快进，第二天上涨必须卖出，急快短线，博个反弹
    private static final String KEY_101 = "stock#" + "get-left-side-5-lian-down-must-sell-next-day";
    // 102、query9ZhuanB，查看了下跌过程中的九转，可能会上涨，也可能继续下跌
    private static final String KEY_102 = "stock#" + "get-left-side-query9ZhuanB";
    // 103、根据最近一年的数据，使用 clac_expma_10/clac_expma_5 进行计算，找出低点，找出TOP3
    private static final String KEY_103 = "stock#" + "get-left-side-expma10-expma5-top3";
    // 104、根据最近一年的数据，使用 clac_expma_10/clac_expma_5 进行计算，找出低点，找出TOP3，历史上30天记录，其实就是第1点的历史数据
    private static final String KEY_104 = "stock#" + "get-left-side-expma10-expma5-top3-history-30days";
    // 105、统计最近一年，主要指数的跌幅TOP12
    private static final String KEY_105 = "stock#" + "get-left-side-index-top12-1-year";


    // 221、综合考虑，可以下手了：1)必须站上5日均线;2)禁止出现上引线；3）5、10、20 必须多头排列;4)必须出现4日连涨；5）涨幅不能太大
    private static final String KEY_221 = "stock#" + "get-right-side-4-lian-up-AND-20-duo-tou-AND-no-shang-yin";
    // 222、最近10个交易日有冲高，且20均线多头
    private static final String KEY_222 = "stock#" + "get-right-side-large-up-AND-ma-duo-tou-ma-5-10-20";
    // 223、找到最近的冲顶数据
    private static final String KEY_223 = "stock#" + "get-right-side-latest-rise-limit";
    // 224、查询最近一个月的大波动且Vol是短期低点，很可能是上涨中继
    private static final String KEY_224 = "stock#" + "get-right-side-big-swing-and-lowest-vol";
    // 225、查询最近一个月的大波动
    private static final String KEY_225 = "stock#" + "get-right-side-big-swing";
    // 226、成交量相较于前一天上涨3倍，且当天上涨超过3%
    private static final String KEY_226 = "stock#" + "get-right-side-volumn-suddenly-rised-tiple-next-day";
    // 227、近3个月的，成交量暴涨9倍的，可以观察，说不定可以追
    private static final String KEY_227 =  "stock#" + "get-right-side-volumn-rised-9x-in-past-90-days";
    // 228、跳空高开，等回调
    private static final String KEY_228 = "stock#" + "get-right-side-up-jump-recently";
    // 229、25年后有过3倍的涨幅，且存在短期快速上涨
    private static final String KEY_229 = "stock#" + "get-right-side-up-fast";
    // 230、5连up
    private static final String KEY_230 = "stock#" + "get-right-side-5-lian-up";
    // 231、query9ZhuanS，查看了上涨过程中的九转，可能会下跌，也可能继续上涨
    private static final String KEY_231 = "stock#" + "get-right-side-query9ZhuanS";
    // 232、查询多头排列的票(ma多头)
    private static final String KEY_232 = "stock#" + "get-right-side-duo-tou-ma";
    // 233、查询多头排列的票(5>10>20>60)
    private static final String KEY_233 = "stock#" + "get-right-side-duo-tou";
    // 234、收盘在20日均或者60日均的位置，可能会反弹
    private static final String KEY_234 = "stock#" + "get-right-side-avg20-or-avg60";
    // 235、考虑cci在-100掠过，即只是简单地经过-100，且地量+大振幅的目的
    private static final String KEY_235 = "stock#" + "get-right-side-cci-and-low-vol-and-big-swing";
    // 236、查询最近一个月的大波动且Vol是5天内的最低点，很可能是上涨中继
    private static final String KEY_236 = "stock#" + "get-right-side-big-swing-and-lowest-vol-2";


    /**
     * 10000、
     * @return
     */
    @GetMapping("/easy-snapshot-left")
    public BaseResponse easySnapshotLeft() {
        log.info("easySnapshotLeft start...");
        String cacheKey = KEY_10000;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(cacheKey);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", cacheKey, res);
            return RestGeneralResponse.of(res);
        }

        res = new ArrayList<>();
        Map<String, String> statisRes = new HashMap<>(); // 结构为：601800#--#基础建设-中国交建, KEY_101#KEY_102
        Set<String> fieldsSet = new HashSet<>();

        String startDate = agEastmoneyStockMapper.getLimitDate();
        // 依次计算
        for(String kk : Arrays.asList(KEY_101, KEY_102, KEY_103)) {
            List<SpecialCarePoJo2> res0 = (List<SpecialCarePoJo2>) myCaffeineCache.get(kk);
            log.info("get cache value. key = {}, value = {}", kk, JSON.toJSON(res0));
            if(!CollectionUtils.isEmpty(res0)){
                String kkName = "";
                switch (kk) {
                    case KEY_101: kkName = "KEY_101"; break;
                    case KEY_102: kkName = "KEY_102"; break;
                    case KEY_103: kkName = "KEY_103"; break;
                    default:
                        break;
                }

                String finalKkName = kkName;
                Set<String> strs = res0.stream()
                                    .filter(f -> f.getDate().compareTo(startDate) >= 0)
                                    .map(m -> String.join("#", Arrays.asList(m.getStockCode(), "--", m.getLast(), finalKkName)))
                                    .collect(Collectors.toSet());
                if(!CollectionUtils.isEmpty(strs)) {
                    // 拿到key值的列表
                    fieldsSet.addAll(strs);
                }
            }
        }

        log.info("fieldsSet = {}", JSON.toJSONString(fieldsSet));
        // 将数据转到set里面
        if(!CollectionUtils.isEmpty(fieldsSet)) {
            for(String str : fieldsSet) {
                String[] fields = str.split("#");
                if(fields != null && fields.length >= 4) {
                    String kk = fields[3];
                    String key = str.replace(kk, "");
                    if(!statisRes.containsKey(key)) {
                        statisRes.put(key, kk + "#");
                    } else {
                        statisRes.put(key, statisRes.get(key) + kk + "#");
                    }
                }
            }
        }

        log.info("statisRes = {}", JSON.toJSONString(statisRes));
        // 将set里面的数据进行一个统计
        if(!CollectionUtils.isEmpty(statisRes)) {
            for(Map.Entry<String, String> entry : statisRes.entrySet()) {
                SpecialCarePoJo2 tmpPoJo2 = new SpecialCarePoJo2();
                String key = entry.getKey();
                String[] fileds = key.split("#");
                if(fileds != null && fileds.length >= 3) {
                    tmpPoJo2.setStockCode(fileds[0]);
                    tmpPoJo2.setDate(fileds[1]);
                    tmpPoJo2.setLast(fileds[2]);
                }

                String value = entry.getValue();
                fileds = value.split("#");
                if(fileds.length < 2) {
                    continue;
                }

                if(fileds != null) {
                    tmpPoJo2.setLast(String.format(Locale.ROOT, "%02d", fileds.length) + "#" + tmpPoJo2.getLast());
                    tmpPoJo2.setRatioB(value);
                }
                res.add(tmpPoJo2);
            }
        }

        if(!CollectionUtils.isEmpty(res)) {
            res = res.stream()
                    .sorted(Comparator.comparing(SpecialCarePoJo2::getLast).reversed())
                    .collect(Collectors.toList());
            for(SpecialCarePoJo2 pojo2 : res) {
                String tmp = pojo2.getRatioB();
                String[] splits = tmp.replace("#", "").split("KEY_");
                tmp = String.join("_"
                        , Arrays.stream(splits)
                                .sorted()
                                .filter(f -> !StringUtils.isEmpty(f))
                                .collect(Collectors.toList()));
                pojo2.setRatioB(tmp);
            }
        } else {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            res = Arrays.asList(empty);
        }

        myCaffeineCache.put(cacheKey, res);
        log.info("myCaffeineCache put, key={}, res={}", cacheKey, res);
        return RestGeneralResponse.of(res);
    }


    /**
     * 10001、
     * @return
     */
    @GetMapping("/easy-snapshot-right")
    public BaseResponse easySnapshotRight() {
        log.info("easySnapshotRight start...");
        String cacheKey = KEY_10001;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(cacheKey);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", cacheKey, res);
            return RestGeneralResponse.of(res);
        }

        res = new ArrayList<>();
        Map<String, String> statisRes = new HashMap<>(); // 结构为：601800#--#基础建设-中国交建, KEY_101#KEY_102
        Set<String> fieldsSet = new HashSet<>();

        String startDate = agEastmoneyStockMapper.getLimitDate();
        // 依次计算
        for(String kk : Arrays.asList(
                KEY_221, KEY_222, KEY_223, KEY_224, KEY_225
                , KEY_226, KEY_227, KEY_228, KEY_229, KEY_230
                , KEY_231, KEY_232, KEY_233, KEY_234, KEY_235, KEY_236)) {
            List<SpecialCarePoJo2> res0 = (List<SpecialCarePoJo2>) myCaffeineCache.get(kk);
            log.info("get cache value. key = {}, value = {}", kk, JSON.toJSON(res0));
            if(!CollectionUtils.isEmpty(res0)){
                String kkName = "";
                switch (kk) {
                    case KEY_221: kkName = "KEY_221"; break;
                    case KEY_222: kkName = "KEY_222"; break;
                    case KEY_223: kkName = "KEY_223"; break;
                    case KEY_224: kkName = "KEY_224"; break;
                    case KEY_225: kkName = "KEY_225"; break;
                    case KEY_226: kkName = "KEY_226"; break;
                    case KEY_227: kkName = "KEY_227"; break;
                    case KEY_228: kkName = "KEY_228"; break;
                    case KEY_229: kkName = "KEY_229"; break;
                    case KEY_230: kkName = "KEY_230"; break;
                    case KEY_231: kkName = "KEY_231"; break;
                    case KEY_232: kkName = "KEY_232"; break;
                    case KEY_233: kkName = "KEY_233"; break;
                    case KEY_234: kkName = "KEY_234"; break;
                    case KEY_235: kkName = "KEY_235"; break;
                    case KEY_236: kkName = "KEY_236"; break;
                    default:
                        break;
                }

                String finalKkName = kkName;
                Set<String> strs = res0.stream()
                        .filter(f -> f.getDate().compareTo(startDate) >= 0)
                        .map(m -> String.join("#", Arrays.asList(m.getStockCode(), "--", m.getLast(), finalKkName)))
                        .collect(Collectors.toSet());
                if(!CollectionUtils.isEmpty(strs)) {
                    // 拿到key值的列表
                    fieldsSet.addAll(strs);
                }
            }
        }

        log.info("fieldsSet = {}", JSON.toJSONString(fieldsSet));
        // 将数据转到set里面
        if(!CollectionUtils.isEmpty(fieldsSet)) {
            for(String str : fieldsSet) {
                String[] fields = str.split("#");
                if(fields != null && fields.length >= 4) {
                    String kk = fields[3];
                    String key = str.replace(kk, "");
                    if(!statisRes.containsKey(key)) {
                        statisRes.put(key, kk + "#");
                    } else {
                        statisRes.put(key, statisRes.get(key) + kk + "#");
                    }
                }
            }
        }

        log.info("statisRes = {}", JSON.toJSONString(statisRes));
        // 将set里面的数据进行一个统计
        if(!CollectionUtils.isEmpty(statisRes)) {
            for(Map.Entry<String, String> entry : statisRes.entrySet()) {
                SpecialCarePoJo2 tmpPoJo2 = new SpecialCarePoJo2();
                String key = entry.getKey();
                String[] fileds = key.split("#");
                if(fileds != null && fileds.length >= 3) {
                    tmpPoJo2.setStockCode(fileds[0]);
                    tmpPoJo2.setDate(fileds[1]);
                    tmpPoJo2.setLast(fileds[2]);
                }

                String value = entry.getValue();
                fileds = value.split("#");
                if(fileds.length < 3) {
                    continue;
                }

                if(fileds != null) {
                    tmpPoJo2.setLast(String.format(Locale.ROOT, "%02d", fileds.length) + "#" + tmpPoJo2.getLast());
                    tmpPoJo2.setRatioB(value);
                }
                res.add(tmpPoJo2);
            }
        }

        if(!CollectionUtils.isEmpty(res)) {
            res = res.stream()
                    .sorted(Comparator.comparing(SpecialCarePoJo2::getLast).reversed())
                    .collect(Collectors.toList());
            for(SpecialCarePoJo2 pojo2 : res) {
                String tmp = pojo2.getRatioB();
                String[] splits = tmp.replace("#", "").split("KEY_");
                tmp = String.join("_"
                        , Arrays.stream(splits)
                                .sorted()
                                .filter(f -> !StringUtils.isEmpty(f))
                                .collect(Collectors.toList()));
                pojo2.setRatioB(tmp);
            }
        } else {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            res = Arrays.asList(empty);
        }

        myCaffeineCache.put(cacheKey, res);
        log.info("myCaffeineCache put, key={}, res={}", cacheKey, res);
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
     * 101、
     * * @return
     */
    @GetMapping("/get-left-side-5-lian-down-must-sell-next-day")
    public BaseResponse down5() {
        log.info("down5");
        String key = KEY_101;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.down5();
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
     * 102、
     * @return
     */
    @GetMapping("/get-left-side-query9ZhuanB")
    public BaseResponse query9ZhuanB() {
        log.info("query9ZhuanB");
        String key = KEY_102;
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
     * 103、
     * @return
     */
    @GetMapping("/get-left-side-expma10-expma5-top3")
    public BaseResponse queryEastmoneyToday() {
        log.info("queryEastmoneyToday");
        String key = KEY_103;
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
     * 104、
     * @return
     */
    @GetMapping("/get-left-side-expma10-expma5-top3-history-30days")
    public BaseResponse queryEastmoneyLast30() {
        log.info("specialCareDaysEastmoney");
        String key = KEY_104;
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
     * 105、
     * @return
     */
    @GetMapping("/get-left-side-index-top12-1-year")
    public BaseResponse queryIndexTop12In1Year() {
        log.info("queryIndexTop12In1Year");
        String key = KEY_105;
        List<SpecialCarePoJo> res = (List<SpecialCarePoJo>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryIndexTop12In1Year();
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
     * 221、
     * * @return
     */
    @GetMapping("/get-right-side-4-lian-up-AND-20-duo-tou-AND-no-shang-yin")
    public BaseResponse considerAll() {
        log.info("considerAll");
        String key = KEY_221;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.considerAll();
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
     * 222、
     * * @return
     */
    @GetMapping("/get-right-side-large-up-AND-ma-duo-tou-ma-5-10-20")
    public BaseResponse MA20maSSP() {
        log.info("MA20maSSP");
        String key = KEY_222;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.MA20maSSP();
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
     * 223、
     * * @return
     */
    @GetMapping("/get-right-side-latest-rise-limit")
    public BaseResponse queryLatestRiseLimit() {
        log.info("queryLatestRiseLimit");
        String key = KEY_223;
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
     * 224、
     * * @return
     */
    @GetMapping("/get-right-side-big-swing-and-lowest-vol")
    public BaseResponse queryBigSwingAndLowestVol() {
        log.info("queryBigSwingAndLowestVol");
        String key = KEY_224;
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
     * 225、
     * * @return
     */
    @GetMapping("/get-right-side-big-swing")
    public BaseResponse queryBigSwing() {
        log.info("queryBigSwing");
        String key = KEY_225;
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
     * 226、
     * @return
     */
    @GetMapping("/get-right-side-volumn-suddenly-rised-tiple-next-day")
    public BaseResponse queryEastmoneyVolSuddenlyRisedTriple() {
        log.info("queryEastmoneyVolSuddenlyRisedTriple");
        String key = KEY_226;
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
     * 227、
     * @return
     */
    @GetMapping("/get-right-side-volumn-rised-9x-in-past-90-days")
    public BaseResponse query9VolInLastest90Days() {
        log.info("query9VolInLastest90Days");
        String key = KEY_227;
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
     * 228、
     * * @return
     */
    @GetMapping("/get-right-side-up-jump-recently")
    public BaseResponse jumpAndWait() {
        log.info("jumpAndWait");
        String key = KEY_228;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.jumpAndWait();
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
     * 229、
     * * @return
     */
    @GetMapping("/get-right-side-up-fast")
    public BaseResponse queryOnlyThem() {
        log.info("queryOnlyThem");
        String key = KEY_229;
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
     * 230、
     * * @return
     */
    @GetMapping("/get-right-side-5-lian-up")
    public BaseResponse queryUp5Lian() {
        log.info("queryUp5Lian");
        String key = KEY_230;
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
     * 231、
     * @return
     */
    @GetMapping("/get-right-side-query9ZhuanS")
    public BaseResponse query9ZhuanS() {
        log.info("query9ZhuanS");
        String key = KEY_231;
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
     * 232、
     * * @return
     */
    @GetMapping("/get-right-side-duo-tou-ma")
    public BaseResponse queryDuoTouMA() {
        log.info("queryDuoTouMA");
        String key = KEY_232;
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
     * 233、
     * * @return
     */
    @GetMapping("/get-right-side-duo-tou")
    public BaseResponse queryDuoTou() {
        log.info("queryDuoTou");
        String key = KEY_233;
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

    /**
     * 234、
     * @return
     */
    @GetMapping("/get-right-side-avg20-or-avg60")
    public BaseResponse queryAvg60() {
        log.info("queryAvg60");
        String key = KEY_234;
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
     * 235、
     * @return
     */
    @GetMapping("/get-right-side-cci-and-low-vol-and-big-swing")
    public BaseResponse considerCCIAndVol() {
        log.info("considerCCIAndVol");
        String key = KEY_235;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agCCIEastmoneyStockMapper.considerCCIAndVol();
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
     * 236、
     * * @return
     */
    @GetMapping("/get-right-side-big-swing-and-lowest-vol-2")
    public BaseResponse queryBigSwingAndIn5LowestVol() {
        log.info("queryBigSwingAndIn5LowestVol");
        String key = KEY_236;
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.queryBigSwingAndIn5LowestVol();
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
     * 997、查询上证的大幅下跌
     * @return
     */
    @GetMapping("/eastmoney-get_000001_lowest")
    public BaseResponse get_000001_lowest() {
        log.info("get_000001_lowest");
        String key = "stock#" + "get_000001_lowest";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.get_000001_lowest();
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
     * 998、查询最近的每天的数据量
     * @return
     */
    @GetMapping("/eastmoney-daily-cnt")
    public BaseResponse getDailyCnt() {
        log.info("getDailyCnt");
        String key = "stock#" + "getDailyCnt";
        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
        if(res != null) {
            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
            return RestGeneralResponse.of(res);
        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agEastmoneyStockMapper.getDailyCnt();
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
     * 999、查询每个stock的最近的数据
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

