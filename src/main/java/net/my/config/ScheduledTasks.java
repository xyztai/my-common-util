package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.controller.AgController;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.AgParaBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ScheduledTasks {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private DataCalcMapper dataCalc;

    @Autowired
    private AgController agController;

    @Scheduled(initialDelay = 1000 * 5, fixedRate = 1000 * 3600 * 3)
    public void refreshAgPara() {
        log.info("refreshAgPara begin. now: {}", dateFormat.format(new Date()));
        List<AgParaBO> paras = dataCalc.queryPara();
        List<AgParaBO> maxParas = dataCalc.queryMaxPara();
        for(AgParaBO bo : paras) {
            Optional<AgParaBO> tmp = maxParas.stream().filter(f -> f.getType().equals(bo.getType()) && f.getBRatio() > bo.getBRatio()).findFirst();
            tmp.ifPresent(f -> bo.setBRatio(f.getBRatio()));
            tmp = maxParas.stream().filter(f -> f.getType().equals(bo.getType()) && f.getSRatio() > bo.getSRatio()).findFirst();
            tmp.ifPresent(f -> bo.setSRatio(f.getSRatio()));
            dataCalc.updatePara(bo);
        }
        log.info("refreshAgPara end. now: {}", dateFormat.format(new Date()));
    }


    @Scheduled(cron = "0 0 */4 * * ?")
    @Transactional
    public void execAutoTask() {
        log.info("execAutoTask begin");
        agController.saveDailyPara();
        agController.updatePara();
        log.info("execAutoTask end");
    }

    @Scheduled(cron = "*/5 * * * * ?")
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
        System.out.println("Beijing Time: " + formattedTime);
        if(formattedTime.compareTo("15:01:00") > 0 && formattedTime.compareTo("15:10:00") < 0) {
            log.info("time to execGetHistoryData");
        } else {
            log.info("not time to execGetHistoryData");
        }
        // getHistoryData(5);
        log.info("execGetHistoryData end");
    }
}
