package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.controller.*;
import net.my.mapper.AgWeekMapper;
import net.my.mapper.KLineRealTimeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ScheduledTasks {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private AgController agController;

    @Autowired
    private AgStrategyController agStrategyController;

    @Autowired
    private AgQueryStockController agQueryStockController;

    @Autowired
    private AgCCIController agCCIController;

    @Autowired
    private AgMAController agMAController;

    @Autowired
    private KLineRealTimeController KLineRealTimeController;

    @Autowired
    private AgQueryETFController agQueryETFController;

    @Autowired
    private AgWeekMapper agWeekMapper;

    @Autowired
    private KLineRealTimeMapper kLineRealTimeMapper;

    @Value("${executeOnceTaskEnable}")
    private Boolean executeOnceTaskEnable;

    private void invalidateAll() {
        log.info("invalidateAll...");
        myCaffeineCache.invalidateAll();
    }

    /**
     * 每周日计算每周的数据
     */
    @Scheduled(cron = "0,30 0 0 ? * SUN")
    public void genWeeklyData() {
        log.info("genWeeklyData start");
        agWeekMapper.genWeeklyData();
        log.info("genWeeklyData end");
    }

    /**
     * 每日更新下 KLineRealTime 数据，这个数据是为了做数据补偿的，平时不用的
     */
    @Scheduled(cron = "0 51 * * * ?")
    public void execKLineRealTime() {
        long startTime = System.currentTimeMillis();
        log.info("execKLineRealTime begin");
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
                (formattedTime.compareTo("05:01:00") > 0 && formattedTime.compareTo("05:55:00") < 0) ||  // 这里5点，对应北京时间17点
//                        (formattedTime.compareTo("05:01:00") > 0 && formattedTime.compareTo("05:55:00") < 0) ||
                        (formattedTime.compareTo("17:01:00") > 0 && formattedTime.compareTo("17:55:00") < 0)
//                                || (formattedTime.compareTo("16:01:00") > 0 && formattedTime.compareTo("16:55:00") < 0)
        ) {
            log.info("time to execKLineRealTime");
            // 获取当天的实时数据
            KLineRealTimeController.getDataFromQQ();
            // 清理重复数据
            kLineRealTimeMapper.delDuplicateData();
        } else {
            log.info("not time to execKLineRealTime");
        }
        log.info("execKLineRealTime end");
        log.info("execKLineRealTime Time-Consuming: {} ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 自动获取当前的收盘数据，以及计算常用的指数数据，进行查询缓存
     */
    @Scheduled(cron = "0 37 * * * ?")
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
                (formattedTime.compareTo("05:01:00") > 0 && formattedTime.compareTo("05:55:00") < 0) ||  // 这里5点，对应北京时间17点
//                        (formattedTime.compareTo("05:01:00") > 0 && formattedTime.compareTo("05:55:00") < 0) ||
                        (formattedTime.compareTo("17:01:00") > 0 && formattedTime.compareTo("17:55:00") < 0)
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

    // 启动后，自动做查询动作，进行查询缓存
    @Scheduled(initialDelay = 5000, fixedDelay = 7 * 24 * 3600 * 1000)
    public void executeOnceTask() {
        long startTime = System.currentTimeMillis();
        log.info("Task executed once after 5 seconds");
        log.info("executeOnceTask start...");
        if(!executeOnceTaskEnable) {
            log.info("executeOnceTask skip");
        } else {
            execCalc();
            execQuery();
        }
        log.info("executeOnceTask end...");
        log.info("executeOnceTask Time-Consuming: {} ms", System.currentTimeMillis() - startTime);
    }

    public void triggerOnce() {

        // 获得当天数据
        log.info("getTodayDataStockCode start");
        agController.getTodayDataStockCode();
        log.info("getTodayDataEtf start");
        agController.getTodayDataEtf();
        log.info("getTodayDataIndex start");
        agController.getTodayDataIndex();

        execCalc();
        execQuery();
    }

    /**
     * 1、先计算所有需要的数据
     */
    void execCalc() {
        // 1、此处有计算cci
        log.info("task cci start");
        agCCIController.historyAllStock();
        agCCIController.historyAllEtf();
        log.info("task cci end");

        // 2、此处有计算ma
        log.info("task ma start");
        agMAController.historyAllStock();
        agMAController.historyAllEtf();
        log.info("task ma end");
    }

    /**
     * 2、计算数据后，为查询进行数据缓存
     */
    void execQuery() {

        // 清空缓存
        log.info("task 清空缓存");
        invalidateAll();

        // 开始进行查询缓存
        ExecutorService executor = Executors.newFixedThreadPool(5);
        // 计算缓存

        // 策略2计算，速度慢，需要提前计算
        CompletableFuture<Void> stock_task_88802 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock strategy_2 start");
                agStrategyController.strategy_2();
                log.info("task stock strategy_2 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        // 策略3计算
        CompletableFuture<Void> stock_task_88803 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock strategy_3 start");
                agStrategyController.strategy_3();
                log.info("task stock strategy_3 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        // 策略5计算
        CompletableFuture<Void> stock_task_88805 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock strategy_5 start");
                agStrategyController.strategy_5();
                log.info("task stock strategy_5 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> stock_task_101 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock down5 start");
                agQueryStockController.down5();
                log.info("task stock down5 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_102 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9ZhuanB start");
                agQueryStockController.query9ZhuanB();
                log.info("task stock query9ZhuanB end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_103 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyToday start");
                agQueryStockController.queryEastmoneyToday();
                log.info("task stock queryEastmoneyToday end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_104 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyLast30 start");
                agQueryStockController.queryEastmoneyLast30();
                log.info("task stock queryEastmoneyLast30 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_105 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryIndexTop12In1Year start");
                agQueryStockController.queryIndexTop12In1Year();
                log.info("task stock queryIndexTop12In1Year end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);



        CompletableFuture<Void> stock_task_221 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock considerAll start");
                agQueryStockController.considerAll();
                log.info("task stock considerAll end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> stock_task_222 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock MA20maSSP start");
                agQueryStockController.MA20maSSP();
                log.info("task stock MA20maSSP end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_223 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryLatestRiseLimit start");
                agQueryStockController.queryLatestRiseLimit();
                log.info("task stock queryLatestRiseLimit end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_224 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryBigSwingAndLowestVol start");
                agQueryStockController.queryBigSwingAndLowestVol();
                log.info("task stock queryBigSwingAndLowestVol end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_225 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryBigSwing start");
                agQueryStockController.queryBigSwing();
                log.info("task stock queryBigSwing end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_226 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyVolSuddenlyRisedTriple start");
                agQueryStockController.queryEastmoneyVolSuddenlyRisedTriple();
                log.info("task stock queryEastmoneyVolSuddenlyRisedTriple end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_227 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9VolInLastest90Days start");
                agQueryStockController.query9VolInLastest90Days();
                log.info("task stock query9VolInLastest90Days end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> stock_task_228 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock jumpAndWait start");
                agQueryStockController.jumpAndWait();
                log.info("task stock jumpAndWait end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> stock_task_229 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryOnlyThem start");
                agQueryStockController.queryOnlyThem();
                log.info("task stock queryOnlyThem end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_230 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryUp5Lian start");
                agQueryStockController.queryUp5Lian();
                log.info("task stock queryUp5Lian end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_231 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock query9ZhuanS start");
                agQueryStockController.query9ZhuanS();
                log.info("task stock query9ZhuanS end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_232 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryDuoTouMA start");
                agQueryStockController.queryDuoTouMA();
                log.info("task stock queryDuoTouMA end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_235 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock considerCCIAndVol start");
                agQueryStockController.considerCCIAndVol();
                log.info("task stock considerCCIAndVol end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        CompletableFuture<Void> stock_task_236 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryBigSwingAndIn5LowestVol start");
                agQueryStockController.queryBigSwingAndIn5LowestVol();
                log.info("task stock queryBigSwingAndIn5LowestVol end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);




        CompletableFuture<Void> stock_task_997 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock get_000001_lowest start");
                agQueryStockController.get_000001_lowest();
                log.info("task stock get_000001_lowest end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

//        CompletableFuture<Void> stock_task_998 = CompletableFuture.runAsync(() -> {
//            try {
//                log.info("task stock getDailyCnt start");
//                agNewEastmoneyStockController.getDailyCnt();
//                log.info("task stock getDailyCnt end");
//            } catch (Exception e) {
//                Thread.currentThread().interrupt();
//            }
//        }, executor);

        CompletableFuture<Void> stock_task_999 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task stock queryEastmoneyLatestInfo start");
                agQueryStockController.queryEastmoneyLatestInfo();
                log.info("task stock queryEastmoneyLatestInfo end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);









        // 查询 ETF
        CompletableFuture<Void> etf_task_201 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf investEtfChgTop3 start");
                agQueryETFController.queryEtfChgTop3();
                log.info("task etf investEtfChgTop3 end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        CompletableFuture<Void> etf_task_202 = CompletableFuture.runAsync(() -> {
            try {
                log.info("task etf investEtfChgTop3History start");
                agQueryETFController.queryEtfChgTop3History();
                log.info("task etf investEtfChgTop3History end");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }, executor);


        // 等待所有任务完成
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                stock_task_88802
                , stock_task_88803
                , stock_task_88805
                , stock_task_101
                , stock_task_102
                , stock_task_103
                , stock_task_104
                , stock_task_105
                , stock_task_221
                , stock_task_222
                , stock_task_223
                , stock_task_224
                , stock_task_225
                , stock_task_226
                , stock_task_227
                , stock_task_228
                , stock_task_229
                , stock_task_230
                , stock_task_231
                , stock_task_232
                , stock_task_235
                , stock_task_236
                // 233 因为有计算，所有在查询之前执行
                , stock_task_997
//                , stock_task_998
                , stock_task_999


                , etf_task_201
                , etf_task_202


        );

        // 当所有任务完成后执行
        allTasks.thenRun(() -> {
            System.out.println("所有任务已完成");
        }).join();

        log.info("task agNewEastmoneyStockController.easySnapshotRight start");
        agQueryStockController.easySnapshotRight();
        log.info("task agNewEastmoneyStockController.easySnapshotRight end");



    }
}
