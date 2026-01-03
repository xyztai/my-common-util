package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.config.ScheduledTasks;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/ag-task")
@Slf4j
public class AgTaskController {

    @Autowired
    private ScheduledTasks scheduledTasks;

    @GetMapping("/trigger")
    public BaseResponse trigger() {
        log.info("trigger start");
        scheduledTasks.triggerOnce();
        log.info("trigger end");
        return RestGeneralResponse.OK;
    }

}

