package net.my.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import net.my.util.RandomPasswordGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passwd")
@Slf4j
@Api("生成随机密码工具")
public class PasswordGeneratorController {

    @GetMapping("/{length}/{typesCount}")
    public BaseResponse generatePasswd(@PathVariable("length") Integer length, @PathVariable("typesCount") Integer typesCount) {
        String passwd = RandomPasswordGenerator.generateStrongPassword(length, typesCount);
        log.info("passwd: {}", passwd);
        return RestGeneralResponse.of(passwd);
    }
}
