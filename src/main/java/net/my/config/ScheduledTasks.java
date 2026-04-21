package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.controller.*;
import net.my.mapper.AgEastmoneyEChartsMapper;
import net.my.mapper.AgWeekEastmoneyStockMapper;
import net.my.mapper.DataCalcMapper;
import net.my.mapper.TmpMapper;
import net.my.pojo.EastmoneyWinRatioPOJO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ScheduledTasks {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private DataCalcMapper dataCalc;

    @Autowired
    private AgController agController;

    @Autowired
    private AgNewQQController agNewQQController;

    @Autowired
    private AgNewQQ300Controller agNewQQ300Controller;

    @Autowired
    private AgNewEastmoneyStockController agNewEastmoneyStockController;

    @Autowired
    private TmpMapper tmpMapper;

    @Autowired
    private TmpController tmpController;

    @Autowired
    private AgNewEastmoneyETFController agNewEastmoneyETFController;

    @Autowired
    private AgNewEastmoneyIndexController agNewEastmoneyIndexController;

    @Autowired
    private AgEastmoneyEChartsMapper agEastmoneyEChartsMapper;

    @Autowired
    private AgNewEastmoneyEChartsController agNewEastmoneyEChartsController;

    @Autowired
    private net.my.mapper.AgEastmoneyWinRatioMapper agEastmoneyWinRatioMapper;

    @Autowired
    private AgWeekEastmoneyStockMapper agWeekEastmoneyStockMapper;

    @Value("${executeOnceTaskEnable}")
    private Boolean executeOnceTaskEnable;

    public static Integer taskState = 0; // 为0说明是没人在用，可以执行，如果为1，则不能执行


//    /**
//     * 更新历史参数，以及历史预算数据
//     */
//    @Scheduled(cron = "0 0 */12 * * ?")
//    @Transactional
//    public void execHistoryExpect() {
//        log.info("execHistoryExpect begin");
//        agController.genDailyParaAndHistoryExpect();
//        log.info("execHistoryExpect end");
//    }

//    /**
//     * 更新当前使用的参数数据
//     */
//    @Scheduled(initialDelay = 1000 * 5, fixedRate = 1000 * 3600 * 3)
//    @Transactional
//    public void execUpdatePara() {
//        log.info("execUpdatePara begin");
//        agController.updatePara();
//        log.info("execUpdatePara end");
//    }

//    /**
//     * 自动获取/更新历史上5天的cp数据
//     */
//    @Scheduled(cron = "0 */33 * * * ?")
//    @Transactional
//    public void execGetHistoryData() {
//        log.info("execGetHistoryData begin");
//        // 设置时区为北京
//        LocalDateTime now = LocalDateTime.now();
//        ZoneId beijngZoneId = ZoneId.of("Asia/Shanghai");
//        ZonedDateTime beijingTime = now.atZone(beijngZoneId);
//        // 输出北京时间
//        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
//        String formattedTime = beijingTime.format(formatter);
//        log.info("time: {}", formattedTime);
//        if(
//                        (formattedTime.compareTo("03:01:00") > 0 && formattedTime.compareTo("03:55:00") < 0) ||
//                        (formattedTime.compareTo("06:01:00") > 0 && formattedTime.compareTo("06:55:00") < 0) ||
//                        (formattedTime.compareTo("15:01:00") > 0 && formattedTime.compareTo("15:55:00") < 0) ||
//                        (formattedTime.compareTo("18:01:00") > 0 && formattedTime.compareTo("18:55:00") < 0)
//        ) {
//            log.info("time to execGetHistoryData");
////            agNewQQController.getHistoryData(5);
//            agController.getHistoryData(5);
////            agController.getIndustryHistoryData(5);
//        } else {
//            log.info("not time to execGetHistoryData");
//        }
//        log.info("execGetHistoryData end");
//    }


    /**
     * 计算每周的数据
     */
//    @Scheduled(cron = "0 51,55 * * * ?")
    @Scheduled(cron = "0,30 0 0 ? * SUN")
    public void genWeeklyData() {
        log.info("genWeeklyData start");
        agWeekEastmoneyStockMapper.genWeeklyData();
        log.info("genWeeklyData end");
    }

    /**
     * 自动获取/更新历史上5天的cp数据
     */
    @Scheduled(cron = "0 7 * * * ?")
    public void execGetHistoryDataNew() {
        long startTime = System.currentTimeMillis();
        log.info("execGetHistoryDataNew begin");
        // 设置时区为北京
        LocalDateTime now = LocalDateTime.now();
        ZoneId beijngZoneId = ZoneId.of("Asia/Shanghai");
        ZonedDateTime beijingTime = now.atZone(beijngZoneId);
        // 输出北京时间
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = beijingTime.format(formatter);
        log.info("time: {}", formattedTime);
        if(
                        (formattedTime.compareTo("03:01:00") > 0 && formattedTime.compareTo("03:55:00") < 0) ||  // 这里3点，对应北京时间16点
//                        (formattedTime.compareTo("05:01:00") > 0 && formattedTime.compareTo("05:55:00") < 0) ||
                        (formattedTime.compareTo("15:01:00") > 0 && formattedTime.compareTo("15:55:00") < 0)
//                                || (formattedTime.compareTo("16:01:00") > 0 && formattedTime.compareTo("16:55:00") < 0)
        ) {
            log.info("time to execGetHistoryData");
            triggerOnce();
        } else {
            log.info("not time to execGetHistoryData");
        }
        log.info("execGetHistoryDataNew end");
        log.info("execGetHistoryDataNew Time-Consuming: {} ms", System.currentTimeMillis() - startTime);
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 7 * 24 * 3600 * 1000)
    public void executeOnceTask() {
        long startTime = System.currentTimeMillis();
        log.info("Task executed once after 5 seconds");
        log.info("executeOnceTask start...");
        if(!executeOnceTaskEnable) {
            log.info("executeOnceTask skip");
        } else {
            execCalc();
        }
        log.info("executeOnceTask end...");
        log.info("executeOnceTask Time-Consuming: {} ms", System.currentTimeMillis() - startTime);
    }

    public void triggerOnce() {
        if(taskState != 0) {
            log.info("taskState={},放弃本次执行", taskState);
            return;
        }
        taskState = 1;

        // 获取得到历史数据
        log.info("task 获取stock的历史数据 start");
        agNewEastmoneyStockController.getHistoryData();
        log.info("task 获取stock的历史数据 end");
        log.info("task 获取etf的历史数据 start");
        agNewEastmoneyETFController.getHistoryData();
        log.info("task 获取etf的历史数据 end");
        log.info("task 获取index的历史数据 start");
        agNewEastmoneyIndexController.getHistoryDataOuterSina();
        log.info("task 获取index的历史数据 end");

        execCalc();
        taskState = 0;
    }

    void execCalc() {

        // 1、先计算9倍成交量的数据
        log.info("task stock calcVolMulti9OneDay start");
        tmpController.calcVolMulti9OneDay(null);
        log.info("task stock calcVolMulti9OneDay end");

        // 2、再计算均值
        log.info("task stock calcAvg start");
        tmpController.calcAvg(null);
        log.info("task stock calcAvg end");

        // 3、再计算均值的胜率
        log.info("task stock calcAvgWinRatioOneDay start");
        List<String> datesAvgWinRatio = tmpMapper.getDatesAvgWinRatioDefault();
        if(!CollectionUtils.isEmpty(datesAvgWinRatio)) {
            for(String d : datesAvgWinRatio) {
                log.info("datesAvgWinRatio calcD={}", d);
                tmpController.calcAvgWinRatioOneDay(d);
            }
        }
        log.info("task stock calcAvgWinRatioOneDay end");

        // 清空缓存
        log.info("task 清空缓存");
        agNewQQController.invalidateAll();


        // 4、此处有计算，需要提前进行计算
        log.info("task stock queryDuoTou start");
        agNewEastmoneyStockController.queryDuoTou();
        log.info("task stock queryDuoTou end");

        // 开始进行查询缓存
        ExecutorService executor = Executors.newFixedThreadPool(5);
        // 计算缓存
        CompletableFuture<Void> task101 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyToday start");
                agNewEastmoneyStockController.queryEastmoneyToday();
                log.info("task stock queryEastmoneyToday end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task102 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyLast30 start");
                agNewEastmoneyStockController.queryEastmoneyLast30();
                log.info("task stock queryEastmoneyLast30 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task103 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyVolSuddenlyRisedTriple start");
                agNewEastmoneyStockController.queryEastmoneyVolSuddenlyRisedTriple();
                log.info("task stock queryEastmoneyVolSuddenlyRisedTriple end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task104 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9ZhuanB start");
                agNewEastmoneyStockController.query9ZhuanB();
                log.info("task stock query9ZhuanB end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task105 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9ZhuanS start");
                agNewEastmoneyStockController.query9ZhuanS();
                log.info("task stock query9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task106 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyLatestInfo start");
                agNewEastmoneyStockController.queryEastmoneyLatestInfo();
                log.info("task stock queryEastmoneyLatestInfo end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task107 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9VolInLastest90Days start");
                agNewEastmoneyStockController.query9VolInLastest90Days();
                log.info("task stock query9VolInLastest90Days end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task108 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryAvg60 start");
                agNewEastmoneyStockController.queryAvg60();
                log.info("task stock queryAvg60 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task109 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryLatestRiseLimit start");
                agNewEastmoneyStockController.queryLatestRiseLimit();
                log.info("task stock queryLatestRiseLimit end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task110 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryBigSwing start");
                agNewEastmoneyStockController.queryBigSwing();
                log.info("task stock queryBigSwing end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task111 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryBigSwingAndLowestVol start");
                agNewEastmoneyStockController.queryBigSwingAndLowestVol();
                log.info("task stock queryBigSwingAndLowestVol end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        // 该任务需要先执行，不能放在这里一起执行
//        CompletableFuture<Void> task112 = CompletableFuture.runAsync(() -> {
//            try {
//                log.info("task stock queryDuoTou start");
//                agNewEastmoneyStockController.queryDuoTou();
//                log.info("task stock queryDuoTou end");
//            } catch (Exception e) {
//                Thread.currentThread().interrupt();
//            }
//        }, executor);


        CompletableFuture<Void> task113 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryDuoTouMA start");
                agNewEastmoneyStockController.queryDuoTouMA();
                log.info("task stock queryDuoTouMA end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task114 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryUp5Lian start");
                agNewEastmoneyStockController.queryUp5Lian();
                log.info("task stock queryUp5Lian end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> task115 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryOnlyThem start");
                agNewEastmoneyStockController.queryOnlyThem();
                log.info("task stock queryOnlyThem end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> task116 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock jumpAndWait start");
                agNewEastmoneyStockController.jumpAndWait();
                log.info("task stock jumpAndWait end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> task117 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock MA20maSSP start");
                agNewEastmoneyStockController.MA20maSSP();
                log.info("task stock MA20maSSP end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task118 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock considerAll start");
                agNewEastmoneyStockController.considerAll();
                log.info("task stock considerAll end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task119 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock down5 start");
                agNewEastmoneyStockController.down5();
                log.info("task stock down5 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);



        // 查询 ETF
        CompletableFuture<Void> task201 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf investEtfChgTop3 start");
                agNewEastmoneyETFController.investEtfChgTop3();
                log.info("task etf investEtfChgTop3 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> task202 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf investEtfChgTop3History start");
                agNewEastmoneyETFController.investEtfChgTop3History();
                log.info("task etf investEtfChgTop3History end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task203 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyToday start");
                agNewEastmoneyETFController.queryEastmoneyToday();
                log.info("task etf queryEastmoneyToday end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task204 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyLast60 start");
                agNewEastmoneyETFController.queryEastmoneyLast60();
                log.info("task etf queryEastmoneyLast60 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task205 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyVolSuddenlyRised start");
                agNewEastmoneyETFController.queryEastmoneyVolSuddenlyRised();
                log.info("task etf queryEastmoneyVolSuddenlyRised end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task206 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEtf9ZhuanB start");
                agNewEastmoneyETFController.queryEtf9ZhuanB();
                log.info("task etf queryEtf9ZhuanB end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task207 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEtf9ZhuanS start");
                agNewEastmoneyETFController.queryEtf9ZhuanS();
                log.info("task etf queryEtf9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task208 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyLatestInfo start");
                agNewEastmoneyETFController.queryEastmoneyLatestInfo();
                log.info("task etf queryEastmoneyLatestInfo end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task209 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEtfLastest90Days start");
                agNewEastmoneyETFController.queryEtfLastest90Days();
                log.info("task etf queryEtfLastest90Days end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);



//        log.info("task stock queryWinRatios start");
//        agNewEastmoneyStockController.queryWinRatios();
//        log.info("task stock queryWinRatios end");

//        log.info("task etf queryWinRatios start");
//        agNewEastmoneyETFController.queryWinRatios();
//        log.info("task etf queryWinRatios end");



        // 查询图标数据
        log.info("echarts stock getBeginDate start");
        String beginDate = agEastmoneyEChartsMapper.getBeginDate();
        log.info("echarts stock getBeginDate end");

        CompletableFuture<Void> task301 = CompletableFuture.runAsync(() -> {
            try {
                log.info("echarts stock saveEcharts9ZhuanS start");
                agEastmoneyEChartsMapper.saveEcharts9ZhuanS(beginDate);
                log.info("echarts stock saveEcharts9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task302 = CompletableFuture.runAsync(() -> {
            try {
                log.info("echarts stock saveEcharts9ZhuanB start");
                agEastmoneyEChartsMapper.saveEcharts9ZhuanB(beginDate);
                log.info("echarts stock saveEcharts9ZhuanB end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        // 等待所有任务完成
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                task101
                , task102
                , task103
                , task104
                , task105
                , task106
                , task107
                , task108
                , task109
                , task110
                , task111
//                , task112
                , task113
                , task114
                , task115
                , task116
                , task117
                , task118
                , task119

                , task201
                , task202
                , task203
                , task204
                , task205
                , task206
                , task207
                , task208
                , task209

                , task301
                , task302
        );

        // 当所有任务完成后执行
        allTasks.thenRun(() -> {
            System.out.println("所有任务已完成");
        }).join();

        log.info("task echarts s69 start");
        agNewEastmoneyEChartsController.s69();
        log.info("task echarts s69 end");






//            agNewQQController.getHistoryData(5);
//            agNewQQ300Controller.getHistoryData(5);  // QQ 的更新hs300 的
//            agNew300UsingEastmoneyController.getHistoryData();
//            agController.getHistoryData(5);
//            agController.getIndustryHistoryData(5);

        // 计算ratio
//        List<EastmoneyWinRatioPOJO> allRatio = new ArrayList<>();
//        log.info("task etf query9ZhuanB_Copy start");
//        List<EastmoneyWinRatioPOJO> B_09_STOCK = agEastmoneyWinRatioMapper.query9ZhuanB_Copy();
//        log.info("task etf query9ZhuanB_Copy end");
//        if(!CollectionUtils.isEmpty(B_09_STOCK)) {
//            allRatio.addAll(B_09_STOCK);
//        }
//        log.info("task etf query9ZhuanS_Copy start");
//        List<EastmoneyWinRatioPOJO> S_07_STOCK = agEastmoneyWinRatioMapper.query9ZhuanS_Copy();
//        log.info("task etf query9ZhuanS_Copy end");
//        if(!CollectionUtils.isEmpty(S_07_STOCK)) {
//            allRatio.addAll(S_07_STOCK);
//        }
//        log.info("task etf queryEtf9ZhuanB_Copy start");
//        List<EastmoneyWinRatioPOJO> B_09_ETF = agEastmoneyWinRatioMapper.queryEtf9ZhuanB_Copy();
//        log.info("task etf queryEtf9ZhuanB_Copy end");
//        if(!CollectionUtils.isEmpty(B_09_ETF)) {
//            allRatio.addAll(B_09_ETF);
//        }
//        log.info("task etf queryEtf9ZhuanS_Copy start");
//        List<EastmoneyWinRatioPOJO> S_07_ETF = agEastmoneyWinRatioMapper.queryEtf9ZhuanS_Copy();
//        log.info("task etf queryEtf9ZhuanS_Copy end");
//        if(!CollectionUtils.isEmpty(S_07_ETF)) {
//            allRatio.addAll(S_07_ETF);
//        }
//        if(!CollectionUtils.isEmpty(allRatio)) {
//            agEastmoneyWinRatioMapper.delWinRatio();
//            // 将数据插入表中
//            agEastmoneyWinRatioMapper.saveWinRatio(allRatio);
//        }
    }
}
