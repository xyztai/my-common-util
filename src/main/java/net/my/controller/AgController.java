package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.my.interceptor.CurrentUser;
import net.my.interceptor.LoginRequired;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ag")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgController {

    static Map<String, String> map = new LinkedHashMap<>();

    static {
        map.put("sz50", "sh000016");
        map.put("szzs", "sh000001");
        map.put("hs300", "sz399300");
        map.put("szcz", "sz399001");
        map.put("kc50", "sh000688");
        map.put("zz1000", "sh000852");
        map.put("zz2000", "sz399303");
        map.put("bz50", "bj899050");
        map.put("hskjzs", "hkHSTECH");
        map.put("nsdk100", "usNDX");
        // map.put("zq", "sh600030"); // 中信证券
        // map.put("ysjs", "sh000819");
        // map.put("gfcy", "sh601012"); // 隆基
        // map.put("ktjg", "sz930875");
        // map.put("rjzs", "sh012637");
        // map.put("hbyqC", "sh007844");
        map.put("ljln", "sh601012"); // 隆基绿能
        map.put("ndsd", "sz300750"); // 宁德时代
        map.put("ymkd", "sh603259"); // 药明康德
        map.put("tqly", "sz002466"); // 天齐锂业
    }

    /*
    sz50	上证50
    szzs	上证指数
    hs300	沪深300
    szcz	深证成指
    kc50	科创50
    zz1000	中证1000
    zz2000	中证2000
    bz50	北证50
    hskjzs	恒生科技
    zq	    证券
    ysjs	有色金属
    gfcy	光伏产业
    ktjg	空天军工
    rjzs	软件指数
    hbyqC	华宝油气C
    nsdk100	纳斯达克100
     */

    @Autowired
    private DataCalcMapper dataCalc;

    @Autowired
    private RestTemplate restTemplate;

    public static final String URL_FORMAT = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?_var=kline_dayqfq&param=%s,day,,,%d,qfq";

    public static final String[] TYPES = {"sh000001"};

    public static final int HISTORY_DAYS = 320;

    public static final int DAYS_CNT = 1;

    @GetMapping("/history/{days}")
    @Transactional
    public BaseResponse getHistoryData(@PathVariable("days") Integer days) {
        List<AgClosePriceDTO> agClosePriceDTOs = new ArrayList<>();
        for(Map.Entry<String, String> entry : map.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(URL_FORMAT, zqdm, days);
            String res = restTemplate.getForObject(url, String.class);
            assert res != null;
            res = res.replaceAll("kline_dayqfq=", "");
            log.info("res: {}", res);
            String data = JSON.parseObject(res).getString("data");
            String typeData = JSON.parseObject(data).getString(zqdm);
            String dayData = JSON.parseObject(typeData).getString("day");
            List list = JSON.parseObject(dayData, List.class);
            if(list == null) {
                dayData = JSON.parseObject(typeData).getString("qfqday");
                list = JSON.parseObject(dayData, List.class);
            }
            for(Object obj : list) {
                log.info(JSON.toJSONString(obj));
                List innerList = JSON.parseObject(JSON.toJSONString(obj), List.class);
                String time = ((String)innerList.get(0)).replaceAll("\"", "");
                Double oP = Double.parseDouble (((String)innerList.get(1)).replaceAll("\"", ""));
                Double cP = Double.parseDouble (((String)innerList.get(2)).replaceAll("\"", ""));
                Double hP = Double.parseDouble (((String)innerList.get(3)).replaceAll("\"", ""));
                Double lP = Double.parseDouble (((String)innerList.get(4)).replaceAll("\"", ""));
                if(agClosePriceDTOs.stream().map(AgClosePriceDTO::getTime).collect(Collectors.toList()).contains(time)) {
                    AgClosePriceDTO dto = agClosePriceDTOs.stream().filter(f -> time.equals(f.getTime())).findAny().get();
                    dto.setValue(entry.getKey(), cP);
                } else {
                    AgClosePriceDTO dto = new AgClosePriceDTO();
                    dto.setTime(time);
                    dto.setValue(entry.getKey(), cP);
                    agClosePriceDTOs.add(dto);
                }
            }
        }

        agClosePriceDTOs = agClosePriceDTOs.stream().filter(f -> "2023-01-03".compareTo(f.getTime()) < 0).collect(Collectors.toList());
        log.info("agClosePriceDTOs :{}", agClosePriceDTOs);
        String timeMin = agClosePriceDTOs.stream().map(AgClosePriceDTO::getTime).sorted().findFirst().get();
        log.info("timeMin: {}", timeMin);
        agClosePriceDTOs.stream().filter(f -> !timeMin.equals(f.getTime())).forEach(this::onlyAddData);
        agClosePriceDTOs.stream().filter(f -> timeMin.equals(f.getTime())).forEach(this::addData);
        return RestGeneralResponse.of(agClosePriceDTOs);
    }


    @LoginRequired
    @DeleteMapping("/remove/{time}")
    @Transactional
    public BaseResponse remove(@CurrentUser UserBase userBase, @PathVariable("time") String time) {
        log.info("userBase: {}", userBase);
        log.info("remove-time:{}", time);
        dataCalc.deleteCP(time);
        calc(time);
        return RestGeneralResponse.of("删除完成");
    }

    @GetMapping("/para")
    public BaseResponse queryPara() {
        return RestGeneralResponse.of(dataCalc.queryPara());
    }

    @PostMapping("/updatePara")
    @Transactional
    public BaseResponse updatePara() {
        log.info("updatePara begin");
        List<AgParaBO> paras = dataCalc.queryPara();
        List<AgParaBO> maxParas = dataCalc.queryMaxPara();
        for(AgParaBO bo : paras) {
            Optional<AgParaBO> tmp = maxParas.stream().filter(f -> f.getType().equals(bo.getType()) /*&& f.getBRatio() > bo.getBRatio()*/).findFirst();
            tmp.ifPresent(f -> bo.setBRatio(f.getBRatio()));
            tmp = maxParas.stream().filter(f -> f.getType().equals(bo.getType()) /*&& f.getSRatio() > bo.getSRatio()*/).findFirst();
            tmp.ifPresent(f -> bo.setSRatio(f.getSRatio()));
            dataCalc.updatePara(bo);
        }
        log.info("updatePara end");
        return queryPara();
    }

    /**
     * 只计算往前推250天的数据，根据这250天的数据计算得到当天的参数
     * @return
     */
    @PostMapping("/saveDailyPara")
    @Transactional
    public BaseResponse saveDailyPara() {
        log.info("saveDailyPara begin");
        dataCalc.deleteDailyPara();
        dataCalc.saveDailyPara();
        log.info("saveDailyPara end");
        return BaseResponse.OK;
    }

    @Scheduled(cron = "0 0 */4 * * ?")
    @Transactional
    public void execAutoTask() {
        log.info("execAutoTask begin");
        saveDailyPara();
        updatePara();
        log.info("execAutoTask end");
    }


    @GetMapping("/data/cp/{time}")
    public BaseResponse queryCP(@PathVariable("time") String time) {
        List<AgClosePriceBO> bos = dataCalc.queryCP(time);

        Map<String, Double> retMap = new LinkedHashMap<>();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> retMap.put(f.getName(), f.getClosePrice())
            );
        }

        return RestGeneralResponse.of(retMap);
    }

    @GetMapping("/data/cnt")
    public BaseResponse queryDataCnt() {
        List<AgDataCntBO> bos = dataCalc.queryDataCnt();

        return RestGeneralResponse.of(bos);
    }

    @ApiOperation(value = "获取计算数据", notes = "根据日期获取数据")
    @ApiImplicitParam(name = "time", value = "日期", required = true, dataType = "String")
    @GetMapping("/data/calc/{time}")
    public BaseResponse queryDataCalc(@PathVariable("time") String time) {
        List<AgDataCalcBO> bos = dataCalc.queryDataCalc(time);
        bos = bos.stream().sorted(Comparator.comparingDouble(AgDataCalcBO::getMaxCompare).reversed()).collect(Collectors.toList());

        List<String> retList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(bos)) {
            retList = bos.stream().map(m -> String.format("%s %s: expma5=%.2f, expma37=%.2f, maxCompare=%.4f%s sRatio=%.4f%s%s(para=%.3f, pre=%.4f), bRatio=%.4f%s%s(para=%.3f, pre=%.4f)"
                    , m.getDirection()
                    , m.getName(), m.getExpma5(), m.getExpma37()
                    , m.getMaxCompare(), " %"
                    , m.getSRatio(), m.getSRatio()>=1 && m.getSRatio() >= m.getSRatioPre() ? " ↑↑↑ " : ""
                    , m.getSRatio()>=1 && m.getSRatio() >= m.getSRatioPara() ? " *** " : ""
                    , m.getSRatioPara(), m.getSRatioPre()
                    , m.getBRatio(), m.getBRatio()>=1 && m.getBRatio() >= m.getBRatioPre() ? " ↑↑↑ " : ""
                    , m.getBRatio()>=1 && m.getBRatio() >= m.getBRatioPara() ? " *** " : ""
                    , m.getBRatioPara(), m.getBRatioPre())).collect(Collectors.toList());
        }

        return RestGeneralResponse.of(retList);
    }

    @ApiOperation(value = "获取接下来的操作", notes = "需要给出波动值")
    @GetMapping("/expect/hard/{change}")
    @Transactional
    public BaseResponse expectHard(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectHard: time={}, change={}", time, change);
        if(dataCalc.getMaxTime().compareTo(time) >= 0) {
            return RestGeneralResponse.of(String.format("已存在日期大于或等于 %s 的数据，无需预测~", time));
        }

        List<AgClosePriceBO> agClosePriceBOList = dataCalc.getExpectCP(time, change);
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        dataCalc.insertCP(f);
                    }
            );

            calc(time);
            BaseResponse response = queryHardOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    @ApiOperation(value = "获取接下来的操作", notes = "需要给出波动值")
    @GetMapping("/expect/simple/{change}")
    @Transactional
    public BaseResponse expectSimple(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectSimple: time={}, change={}", time, change);
        if(dataCalc.getMaxTime().compareTo(time) >= 0) {
            return RestGeneralResponse.of(String.format("已存在日期大于或等于 %s 的数据，无需预测~", time));
        }

        List<AgClosePriceBO> agClosePriceBOList = dataCalc.getExpectCP(time, change);
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        dataCalc.insertCP(f);
                    }
            );

            calc(time);
            BaseResponse response = querySimpleOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    private void onlyAddData(AgClosePriceDTO agClosePriceDTO) {
        List<AgClosePriceBO> agClosePriceBOList = agClosePriceDTO.toBO();
        agClosePriceBOList = agClosePriceBOList.stream().filter(f -> !StringUtils.isEmpty(f.getClosePrice())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        // 先删后插
                        dataCalc.deleteOneCP(f.getTime(), f.getType());
                        dataCalc.insertCP(f);
                    }
            );
        }
    }

    @PostMapping("/add")
    @Transactional
    public BaseResponse addData(@RequestBody AgClosePriceDTO agClosePriceDTO) {
        log.info("agClosePriceDTO:{}", JSON.toJSONString(agClosePriceDTO));
        List<AgClosePriceBO> agClosePriceBOList = agClosePriceDTO.toBO();
        agClosePriceBOList = agClosePriceBOList.stream().filter(f -> !StringUtils.isEmpty(f.getClosePrice())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        // 先删后插
                        dataCalc.deleteOneCP(f.getTime(), f.getType());
                        dataCalc.insertCP(f);
                    }
            );

            calc(agClosePriceDTO.getTime());
            return RestGeneralResponse.of("数据添加完成");
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    private void calc(String time) {
        // 删掉比插入的数据时间大的所有数据
        dataCalc.deleteDataCalcAfter(time);

        // 重新计算后续所有的数据
        List<String> times = dataCalc.getUnCalcTimes();

        log.info("times:{}", times);
        times.stream().sorted().forEach(dataCalc::insertDataCalc);
    }

    @GetMapping("/oper/hard")
    public BaseResponse queryHardOper() {
        log.info("queryHardOper.");
        List<AgOper> opers = dataCalc.queryHardOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }

        return RestGeneralResponse.of(makeMap(opers));
    }

    @GetMapping("/oper/simple")
    public BaseResponse querySimpleOper() {
        log.info("querySimpleOper.");
        List<AgOper> opers = dataCalc.querySimpleOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }

        return RestGeneralResponse.of(makeMap(opers));
    }

    private Map<String, Map<String, List>> makeMap(List<AgOper> opers) {
        Map<String, Map<String, List>> retMap = new LinkedHashMap<>();
        if(CollectionUtils.isEmpty(opers)) {
            return retMap;
        }

        Comparator<String> comp = (String::compareTo);
        List<String> times = opers.stream().map(AgOper::getTime).distinct().sorted(comp.reversed()).collect(Collectors.toList());
        if(!times.contains("9999-99-99")) {
            retMap.put("预期操作", null);
        }
        log.info("opers: {}", opers);
        for(String time : times) {
            Map<String, List> timeMap = new LinkedHashMap<>();
            List<String> buyList = opers.stream().filter(f -> time.equals(f.getTime()) && "buy".equals(f.getOperDir()))
                    .map(m -> m.getName() + ": " + m.getBuyOper().replaceAll("buy", "买")).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(buyList)) {
                timeMap.put("买:", buyList);
            }

            List<String> sellList = opers.stream().filter(f -> time.equals(f.getTime()) && "sell".equals(f.getOperDir()))
                    .map(m -> m.getName() + ": " + m.getSellOper().replaceAll("sell", "卖")).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(sellList)) {
                timeMap.put("卖:", sellList);
            }

            if(!CollectionUtils.isEmpty(timeMap)) {
                if("9999-99-99".equals(time)) {
                    retMap.put("预期操作", timeMap);
                } else {
                    retMap.put(time, timeMap);
                }
            }
        }
        return retMap;
    }

}
