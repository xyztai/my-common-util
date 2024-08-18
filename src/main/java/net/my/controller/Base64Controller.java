package net.my.controller;

import lombok.extern.slf4j.Slf4j;
import net.my.pojo.BaseResponse;
import net.my.pojo.RestGeneralResponse;
import net.my.util.Base64Util;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b64")
@Slf4j
public class Base64Controller {

    @GetMapping("/url-encode")
    public BaseResponse urlEncode(@RequestParam("str") String str) {
        return RestGeneralResponse.of(Base64Util.urlEncode(str));
    }

    @GetMapping("/url-decode")
    public BaseResponse urlDecode(@RequestParam("b64") String b64) {
        return RestGeneralResponse.of(Base64Util.urlDecode(b64));
    }

}
