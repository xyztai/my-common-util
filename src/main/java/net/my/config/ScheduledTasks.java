package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.controller.*;
import net.my.mapper.AgEastmoneyEChartsMapper;
import net.my.mapper.DataCalcMapper;
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
    private TmpController tmpController;

    @Autowired
    private AgNewEastmoneyETFController agNewEastmoneyETFController;

    @Autowired
    private AgEastmoneyEChartsMapper agEastmoneyEChartsMapper;

    @Autowired
    private AgNewEastmoneyEChartsController agNewEastmoneyEChartsController;

    @Autowired
    private net.my.mapper.AgEastmoneyWinRatioMapper agEastmoneyWinRatioMapper;

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

        execCalc();
        taskState = 0;
    }

    void execCalc() {
        // 清空缓存
        log.info("task 清空缓存");
        agNewQQController.invalidateAll();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        // 计算缓存
        CompletableFuture<Void> task11 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyToday start");
                agNewEastmoneyStockController.queryEastmoneyToday();
                log.info("task stock queryEastmoneyToday end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task12 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyLast30 start");
                agNewEastmoneyStockController.queryEastmoneyLast30();
                log.info("task stock queryEastmoneyLast30 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task13 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyVolSuddenlyRised start");
                agNewEastmoneyStockController.queryEastmoneyVolSuddenlyRised();
                log.info("task stock queryEastmoneyVolSuddenlyRised end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task14 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9ZhuanB start");
                agNewEastmoneyStockController.query9ZhuanB();
                log.info("task stock query9ZhuanB end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task15 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9ZhuanS start");
                agNewEastmoneyStockController.query9ZhuanS();
                log.info("task stock query9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task16 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyLatestInfo start");
                agNewEastmoneyStockController.queryEastmoneyLatestInfo();
                log.info("task stock queryEastmoneyLatestInfo end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task17 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryLastest90Days start");
                agNewEastmoneyStockController.queryLastest90Days();
                log.info("task stock queryLastest90Days end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task18 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock calcAvg start");
                tmpController.calcAvg(null);
                log.info("task stock calcAvg end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);



        // 查询 ETF
        CompletableFuture<Void> task21 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyToday start");
                agNewEastmoneyETFController.queryEastmoneyToday();
                log.info("task etf queryEastmoneyToday end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task22 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyLast60 start");
                agNewEastmoneyETFController.queryEastmoneyLast60();
                log.info("task etf queryEastmoneyLast60 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task23 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyVolSuddenlyRised start");
                agNewEastmoneyETFController.queryEastmoneyVolSuddenlyRised();
                log.info("task etf queryEastmoneyVolSuddenlyRised end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task24 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEtf9ZhuanB start");
                agNewEastmoneyETFController.queryEtf9ZhuanB();
                log.info("task etf queryEtf9ZhuanB end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task25 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEtf9ZhuanS start");
                agNewEastmoneyETFController.queryEtf9ZhuanS();
                log.info("task etf queryEtf9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task26 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf queryEastmoneyLatestInfo start");
                agNewEastmoneyETFController.queryEastmoneyLatestInfo();
                log.info("task etf queryEastmoneyLatestInfo end");
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

        CompletableFuture<Void> task31 = CompletableFuture.runAsync(() -> {
            try {
                log.info("echarts stock saveEcharts9ZhuanS start");
                agEastmoneyEChartsMapper.saveEcharts9ZhuanS(beginDate);
                log.info("echarts stock saveEcharts9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> task32 = CompletableFuture.runAsync(() -> {
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
                task11
                , task12
                , task13
                , task14
                , task15
                , task16
                , task17
                , task18

                , task21
                , task22
                , task23
                , task24
                , task25
                , task26

                , task31
                , task32
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
