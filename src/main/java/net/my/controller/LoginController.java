package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.UserBase;
import net.my.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@Slf4j
public class LoginController {

    @Autowired
    private DataCalcMapper dataCalc;

    @PostMapping
    public String login(@RequestBody UserBase base) {
        if("tai".equals(base.getName()) && "tai".equals(base.getPassword())) {
            return TokenUtils.createJwtToken("No.1");
        }
        return "";
    }
}
