package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.controller.AgController;
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

    /**
     * 更新历史参数，以及历史预算数据
     */
    @Scheduled(cron = "0 0 */12 * * ?")
    @Transactional
    public void execHistoryExpect() {
        log.info("execHistoryExpect begin");
        agController.genDailyParaAndHistoryExpect();
        log.info("execHistoryExpect end");
    }

    /**
     * 更新当前使用的参数数据
     */
    @Scheduled(initialDelay = 1000 * 5, fixedRate = 1000 * 3600 * 3)
    @Transactional
    public void execUpdatePara() {
        log.info("execUpdatePara begin");
        agController.updatePara();
        log.info("execUpdatePara end");
    }

    /**
     * 自动获取/更新历史上5天的cp数据
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void execGetHistoryData() {
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
                        (formattedTime.compareTo("03:01:00") > 0 && formattedTime.compareTo("03:13:00") < 0) ||
                        (formattedTime.compareTo("06:01:00") > 0 && formattedTime.compareTo("06:13:00") < 0) ||
                        (formattedTime.compareTo("15:01:00") > 0 && formattedTime.compareTo("15:13:00") < 0) ||
                        (formattedTime.compareTo("18:01:00") > 0 && formattedTime.compareTo("18:13:00") < 0)
        ) {
            log.info("time to execGetHistoryData");
            agController.getHistoryData(5);
            agController.getIndustryHistoryData(5);
        } else {
            log.info("not time to execGetHistoryData");
        }
        log.info("execGetHistoryData end");
    }
}
