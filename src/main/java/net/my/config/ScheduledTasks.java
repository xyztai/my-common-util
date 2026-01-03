package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.controller.*;
import net.my.mapper.DataCalcMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
    private AgNewEastmoneyETFController agNewEastmoneyETFController;


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
    @Transactional
    public void execGetHistoryDataNew() {
        log.info("execGetHistoryData begin");
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
        log.info("execGetHistoryData end");
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

        // 清空缓存
        log.info("task 清空缓存");
        agNewQQController.invalidateAll();
        // 计算缓存
        log.info("task stock queryEastmoneyToday start");
        agNewEastmoneyStockController.queryEastmoneyToday();
        log.info("task stock queryEastmoneyToday end");

        log.info("task stock queryEastmoneyLast30 start");
        agNewEastmoneyStockController.queryEastmoneyLast30();
        log.info("task stock queryEastmoneyLast30 end");

        log.info("task stock queryEastmoneyVolSuddenlyRised start");
        agNewEastmoneyStockController.queryEastmoneyVolSuddenlyRised();
        log.info("task stock queryEastmoneyVolSuddenlyRised end");

        log.info("task stock queryEastmoneyLatestInfo start");
        agNewEastmoneyStockController.queryEastmoneyLatestInfo();
        log.info("task stock queryEastmoneyLatestInfo end");


        log.info("task etf queryEastmoneyToday start");
        agNewEastmoneyETFController.queryEastmoneyToday();
        log.info("task etf queryEastmoneyToday end");

        log.info("task etf queryEastmoneyLast60 start");
        agNewEastmoneyETFController.queryEastmoneyLast60();
        log.info("task etf queryEastmoneyLast60 end");

        log.info("task etf queryEastmoneyVolSuddenlyRised start");
        agNewEastmoneyETFController.queryEastmoneyVolSuddenlyRised();
        log.info("task etf queryEastmoneyVolSuddenlyRised end");

        log.info("task etf queryEastmoneyLatestInfo start");
        agNewEastmoneyETFController.queryEastmoneyLatestInfo();
        log.info("task etf queryEastmoneyLatestInfo end");

//            agNewQQController.getHistoryData(5);
//            agNewQQ300Controller.getHistoryData(5);  // QQ 的更新hs300 的
//            agNew300UsingEastmoneyController.getHistoryData();
//            agController.getHistoryData(5);
//            agController.getIndustryHistoryData(5);

        taskState = 0;

    }
}
