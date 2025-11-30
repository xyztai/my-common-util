package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-new")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewController {

    static Map<String, String> PROXY_FINANCE_QQ = new LinkedHashMap<>();
    public static final String PROXY_FINANCE_QQ_URL_FORMAT = "https://proxy.finance.qq.com/cgi/cgi-bin/stockinfoquery/kline/app/get?code=%s&ktype=day&limit=%d";
    // demo
//    public static final String URL_FORMAT = "https://proxy.finance.qq.com/cgi/cgi-bin/stockinfoquery/kline/app/get?code=sh512880&ktype=day&limit=500";

    static {
        PROXY_FINANCE_QQ.put("恒生科技ETF-513130", "sh513130");
        PROXY_FINANCE_QQ.put("医疗创新EFT-516820", "sh516820");
        PROXY_FINANCE_QQ.put("上证50EFT-510050", "sh510050");
        PROXY_FINANCE_QQ.put("科创50EFT-588000", "sh588000");
        PROXY_FINANCE_QQ.put("沪深300EFT-510300", "sh510300");
        PROXY_FINANCE_QQ.put("光伏EFT-515790", "sh515790");
        PROXY_FINANCE_QQ.put("纳斯达克100EFT-159659", "sz159659");
        PROXY_FINANCE_QQ.put("华宝油气LOF-162411", "sz162411");
        PROXY_FINANCE_QQ.put("空天军工LOF-160643", "sz160643");
        PROXY_FINANCE_QQ.put("科创芯片EFT-588200", "sh588200");
        PROXY_FINANCE_QQ.put("煤炭ETF-515220", "sh515220");
        PROXY_FINANCE_QQ.put("有色金属ETF-512400", "sh512400");
        PROXY_FINANCE_QQ.put("交通银行-601328", "sz601328");
        PROXY_FINANCE_QQ.put("农业银行-601288", "sz601288");
        PROXY_FINANCE_QQ.put("工商银行-601398", "sz601398");
        PROXY_FINANCE_QQ.put("中国银行-601988", "sz601988");
        PROXY_FINANCE_QQ.put("邮储银行-601658", "sz601658");
        PROXY_FINANCE_QQ.put("京沪高铁-601816", "sz601816");
    }

    @Autowired
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataCalcMapper dataCalcMapper;

    @Autowired
    private RestTemplate restTemplate;


    private Double getScaleDouble(Double dou, int scale) {
        BigDecimal bd = new BigDecimal(dou);
        return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    @ApiOperation(value = "获取历史的cp数据", notes = "访问互联网接口获取数据")
    @ApiImplicitParam(name = "days", value = "制定历史上最近N天的数据", required = true, dataType = "String")
    @GetMapping("/history/{days}")
    @Transactional
    public BaseResponse getHistoryData(@PathVariable("days") Integer days) {
        List<AgClosePriceDTO> agClosePriceDTOs = new ArrayList<>();
        Map<String, QqNode> qqNodeMap = new LinkedHashMap<>();
        for(Map.Entry<String, String> entry : PROXY_FINANCE_QQ.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(PROXY_FINANCE_QQ_URL_FORMAT, zqdm, days);
            log.info("url: {}, zqdm: {}, days: {}", url, zqdm, days);
            String res = "";
            for(int i = 0; i < 200; i++) {
                try {
                    Thread.sleep(200);
                    log.info("try num={}, zqdm={}, url={}", i, zqdm, url);
                    res = restTemplate.getForObject(url, String.class);
                    if(!StringUtils.isEmpty(res)) {
                        break;
                    }
                } catch (Exception ex) {
                    ;
                }
            }
            if(StringUtils.isEmpty(res)) {
                continue;
            }

            QqRes qqRes = JSON.parseObject(res, QqRes.class);
            qqNodeMap.put(entry.getKey(), qqRes.getData().getNodes().get(0));
        }

        return RestGeneralResponse.of(qqNodeMap);
    }

    @Data
    public class QqRes{
        private Integer code;
        private String message;
        private QqData data;
    }

    @Data
    public class QqData{
        private List<QqNode> nodes;
    }

    @Data
    public class QqNode{
        private Double open;
        private Double last;
        private Double high;
        private Double low;
        private Double volume;
        private Double amount;
    }
}
