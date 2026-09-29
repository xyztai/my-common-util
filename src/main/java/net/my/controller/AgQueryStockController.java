package net.my.controller;

import io.swagger.annotations.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.AgQueryStockMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import net.my.pojo.SpecialCarePoJo;
import net.my.pojo.SpecialCarePoJo2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * stock-根据不同的逻辑来查询数据特征点
 */
@RestController
@RequestMapping("/ag-eastmoney-stock")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgQueryStockController {

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private AgQueryStockMapper agQueryStockMapper;

    // 101、查询出现5连跌，且当天的开盘>收盘、chg<0，可以快进，第二天上涨必须卖出，急快短线，博个反弹
    private static final String KEY_101 = "stock#" + "get-left-side-5-lian-down-must-sell-next-day";
    // 102、query9ZhuanB，查看了下跌过程中的九转，可能会上涨，也可能继续下跌
    private static final String KEY_102 = "stock#" + "get-left-side-query9ZhuanB";
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
    // 235、考虑cci在-100掠过，即只是简单地经过-100，且地量+大振幅的目的
    private static final String KEY_235 = "stock#" + "get-right-side-cci-and-low-vol-and-big-swing";
    // 236、查询最近一个月的大波动且Vol是5天内的最低点，很可能是上涨中继
    private static final String KEY_236 = "stock#" + "get-right-side-big-swing-and-lowest-vol-2";


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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.down5();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.query9ZhuanB();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryIndexTop12In1Year();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.considerAll();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.MA20maSSP();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryLatestRiseLimit();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryBigSwingAndLowestVol();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryBigSwing();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryEastmoneyVolSuddenlyRisedTriple();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.query9VolInLastest90Days();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.jumpAndWait();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryOnlyThem();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryUp5Lian();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.query9ZhuanS();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }



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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryDuoTouMA();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
    }

    /**
     * 235、废止，cci的计算有误，该计算返回空
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = new ArrayList<>();
//        List<SpecialCarePoJo2> buyDataFromEastmoneys = agCCIEastmoneyStockMapper.considerCCIAndVol();
//        buyDataFromEastmoneys = buyDataFromEastmoneys.stream()
//                .filter(f -> !f.getStockCode().startsWith("688")
//                        && !f.getStockCode().startsWith("689")
//                        && !f.getStockCode().startsWith("300")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(buyDataFromEastmoneys)) {
            SpecialCarePoJo2 empty = new SpecialCarePoJo2();
            empty.setDate("--");
            empty.setStockCode("--");
            empty.setRatioB("--");
            empty.setLast("--");
            buyDataFromEastmoneys = Arrays.asList(empty);
        }

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryBigSwingAndIn5LowestVol();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.get_000001_lowest();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
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
//        String key = "stock#" + "getDailyCnt";
//        List<SpecialCarePoJo2> res = (List<SpecialCarePoJo2>) myCaffeineCache.get(key);
//        if(res != null) {
//            log.info("myCaffeineCache get, key={}, cacheRes={}", key, res);
//            return RestGeneralResponse.of(res);
//        }

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.getDailyCnt();
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

//        myCaffeineCache.put(key, buyDataFromEastmoneys);
//        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
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

        List<SpecialCarePoJo2> buyDataFromEastmoneys = agQueryStockMapper.queryEastmoneyLatestInfo();
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

        buyDataFromEastmoneys.forEach(f -> f.setRatioB(f.getRatioB().replaceAll("=-", "=-----")));
        myCaffeineCache.put(key, buyDataFromEastmoneys);
        log.info("myCaffeineCache put, key={}, res={}", key, buyDataFromEastmoneys);
        return RestGeneralResponse.of(buyDataFromEastmoneys);
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

