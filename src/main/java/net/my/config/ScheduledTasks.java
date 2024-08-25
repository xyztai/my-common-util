package net.my.config;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.AgParaBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ScheduledTasks {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private DataCalcMapper dataCalc;

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
}
