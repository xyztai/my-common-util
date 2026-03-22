package net.my.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/ag-eastmoney-stock")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyHyController {

    // 获取行业列表
    public static final String EASTMONEY_URL_HY_LIST =
            "https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&fs=m%3A90%2Bs%3A4%2Bf%3A!50&fields=f12%2Cf14&fid=f3&pn=1&pz=200&po=1&dect=1";

    // 获取特定行业对应的code
    // 举例：https://push2.eastmoney.com/api/qt/clist/get?fs=b%3ABK1015&fields=f12%2Cf14&pn=1&pz=2000
    public static final String EASTMONEY_URL_HY_FROMAT =
            "https://push2.eastmoney.com/api/qt/clist/get?fs=b%3A%s&fields=f12%2Cf14&pn=1&pz=2000";


    @Autowired
    private RestTemplate restTemplate;


    @ApiOperation(value = "获取行业数据", notes = "访问互联网接口获取数据")
    @GetMapping("/hy-data")
    @Transactional
    public BaseResponse getHyData() {
        log.info("getHyData start");
        String hyListUrl = EASTMONEY_URL_HY_LIST;
        log.info("call hyListUrl={}", hyListUrl);
        String res = restTemplate.getForObject(hyListUrl, String.class);
        log.info("call hyListUrl={}, res={}", hyListUrl, res);
        JSONObject jsonObject = JSONObject.parseObject(res);
        JSONObject joData = jsonObject.getJSONObject("data");
//        JSONObject joDiff = joData.getJSONObject("diff");
//        for(Map.Entry<String, Object> item : joDiff.entrySet()){
//            log.info("{}", JSON.toJSONString(item.getValue()));
//        }
        JSONArray jsonArray = joData.getJSONArray("diff");
        jsonArray.getJSONObject(0);





//
//        String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);
//        log.info("try num={}, stockCode={}, url={}", i, entry.getKey(), url);
//        String res = restTemplate.getForObject(url, String.class);
//
//        log.info("res={}", res);
//        EastmoneyStockRes eastmoneyRes = JSON.parseObject(res, EastmoneyStockRes.class);
//        log.info("eastmoneyRes={}", JSON.toJSONString(eastmoneyRes));
//


        log.info("getHyData end");
        return BaseResponse.OK;
    }

    @Data
    public class EastmoneyStockRes {
        private EastmoneyStockPOJO data;
    }

    @Data
    public static class EastmoneyStockPOJO {
        /**
         * [
         *                     "2023-04-28", // 日期
         *                     "10.78",  // 开
         *                     "10.99",  // 收
         *                     "11.08",  // 高
         *                     "10.70",  // 低
         *                     "1975714.00", // 量
         *                     {},
         *                     "2.58", // 换手率
         *                     "233884.16", // 金额，单位 万元
         *                     ""
         *                 ]
         */
        private String code;
        private List<String> klines;
    }
}

