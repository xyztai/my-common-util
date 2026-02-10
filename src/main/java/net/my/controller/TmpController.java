package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.TmpMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/avg")
@Slf4j
public class TmpController {

    @Autowired
    private TmpMapper tmpMapper;

    @GetMapping("/calc/{calcDate}")
    public BaseResponse calc(@PathVariable("calcDate") String calcDate) {
        log.info("calc start");
        List<String> dates = tmpMapper.getDates(calcDate);
        if(!CollectionUtils.isEmpty(dates)) {
            for(String d : dates) {
                log.info("calc calcD={}", d);
                calcAvg(d);
            }
        }
        log.info("calc end");
        return RestGeneralResponse.OK;
    }

    public void calcAvg(String calcDate){
        if(StringUtils.isEmpty(calcDate)) {
            Date date = new Date(); // 当前日期
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, 2); // 加上2天
            date = calendar.getTime(); // 获取新的日期

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 定义日期格式
            String dateString = sdf.format(date); // 转换为字符串
            tmpMapper.calcAvg(dateString);
        } else {
            tmpMapper.calcAvg(calcDate);
        }
    }

    @GetMapping("/calcVolMulti9/{calcDate}")
    public BaseResponse calcVolMulti9(@PathVariable("calcDate") String calcDate) {
        log.info("calcVolMulti9 start");
        List<String> dates = tmpMapper.getDatesVolMulti9(calcDate);
        if(!CollectionUtils.isEmpty(dates)) {
            for(String d : dates) {
                log.info("calcVolMulti9 calcD={}", d);
                calcVolMulti9OneDay(d);
            }
        }
        log.info("calcVolMulti9 end");
        return RestGeneralResponse.OK;
    }

    public void calcVolMulti9OneDay(String calcDate){
        if(StringUtils.isEmpty(calcDate)) {
            Date date = new Date(); // 当前日期
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, 2); // 加上2天
            date = calendar.getTime(); // 获取新的日期

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 定义日期格式
            String dateString = sdf.format(date); // 转换为字符串
            tmpMapper.calcVolMulti9(dateString);
        } else {
            tmpMapper.calcVolMulti9(calcDate);
        }
    }

    @GetMapping("/calcAvgWinRatio/{calcDate}")
    public BaseResponse calcAvgWinRatio(@PathVariable("calcDate") String calcDate) {
        log.info("calcAvgWinRatio start");
        List<String> dates = tmpMapper.getDatesAvgWinRatioAll();
        if(!CollectionUtils.isEmpty(dates)) {
            for(String d : dates) {
                log.info("calcVolMulti9 calcD={}", d);
                calcAvgWinRatioOneDay(d);
            }
        }
        log.info("calcAvgWinRatio end");
        return RestGeneralResponse.OK;
    }

    public void calcAvgWinRatioOneDay(String calcDate){
        if(StringUtils.isEmpty(calcDate)) {
           return;
        } else {
            tmpMapper.calcAvgWinRatio(calcDate);
        }
    }
}
