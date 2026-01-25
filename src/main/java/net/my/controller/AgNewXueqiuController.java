package net.my.controller;

import io.swagger.annotations.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.pojo.BaseResponse;
import net.my.pojo.QqNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/ag-xueqiu")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewXueqiuController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/history/{days}")
    @Transactional
    public BaseResponse test() {
        getQQResReplaceEastmoney("SZ000001", 5);
        return BaseResponse.OK;
    }

    // demo
    // https://stock.xueqiu.com/v5/stock/chart/kline.json?symbol=SH600900&begin=1769470679841&period=day&type=before&count=-5&indicator=kline
    public static final String XUE_QIU_URL_FORMAT =
            "https://stock.xueqiu.com/v5/stock/chart/kline.json"
                    +"?symbol=%s&begin=%d&period=day&type=before&count=%d&indicator=kline";
//            "https://proxy.finance.qq.com/cgi/cgi-bin/stockinfoquery/kline/app/get?code=%s&ktype=day&limit=%d";

    public List<String> getQQResReplaceEastmoney(String zqdm, Integer days) {
        Date now = new Date();
        long timestamp = now.getTime() + 1000*3600*24*2;

        List<String> klines = new ArrayList<>();
        // zqdm 举例，000001 为 SZ000001
        String url = String.format(XUE_QIU_URL_FORMAT, zqdm, timestamp, -1 * days);
        log.info("url: {}, zqdm: {}, days: {}", url, zqdm, days);
        String res = "";
        try {
            Thread.sleep(200);
            log.info("try stockCode={}, url={}", zqdm, url);

            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer your-token-here");
            headers.add("Content-Type", "application/json");
            headers.add("User-Agent", "Java-Client/1.0");

            // 创建HttpEntity
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 发送GET请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.info("响应状态: " + response.getStatusCode());
            log.info("响应体: " + response.getBody());

        } catch (Exception ex) {
            ;
        }

        return null;
//        if(StringUtils.isEmpty(res)) {
//            return klines;
//        }
//
//        try {
//            res = res.replace("kline_dayqfq=", "");//.replace(",{},", ",");
//            res = res.substring(0, res.indexOf(",\"qt\"")) + "}}}";
//            log.info("res={}", res);
//            AgNewQQ300Controller.Hs300Res qqRes = JSON.parseObject(res, AgNewQQ300Controller.Hs300Res.class);
////                List<HsStockPoJoJO> pojos = qqRes.getData().get(zqdm).get("qfqday");
////                List<QqNode> tmpNodes = pojos.stream().map(HsStockPoJoJO::toVo).sorted(Comparator.comparing(QqNode::getDate)).collect(Collectors.toList());
//
//            List<List<Object>> pojos = qqRes.getData().get(zqdm).get("qfqday");
//            log.info("res2={}", JSON.toJSON(pojos));
//            if(pojos == null) {
//                pojos = qqRes.getData().get(zqdm).get("day");
//            }
//            if(pojos == null) {
//                return klines;
//            }
//            List<QqNode> tmpNodes = pojos.stream()
//                    .map(AgNewQQ300Controller.HsStockPoJoJO::toVo2)
//                    .sorted(Comparator.comparing(QqNode::getDate))
//                    .collect(Collectors.toList());
//            Double preLast = tmpNodes.get(0).getLast();
//            for(int i = 1; i < tmpNodes.size(); i++) {
//                QqNode node = tmpNodes.get(i);
//                String kline = AgNewQQ300Controller.HsStockPoJoJO.toEastMoneyData(node);
//                // "zhen_fu_ratio" // 振幅=(当天的high-当天的low)/昨天的last*100
//                // "zhang_fu_ratio" // 涨幅=(当天的last-昨天的last)/昨天的last*100
//                // "zhang_fu_zhi" // 涨幅值=当天的last-昨天的last
//                DecimalFormat df2 = new DecimalFormat("0.00");
//                DecimalFormat df3 = new DecimalFormat("0.000");
//                String zhenFuRatio = df2.format((node.getHigh() - node.getLow()) / preLast * 100);
//                String zhangFuRatio = df2.format((node.getLast() - preLast) / preLast * 100);
//                String zhangFuZhi = df3.format(node.getLast() - preLast);
//                kline = kline.replace("zhen_fu_ratio", zhenFuRatio);
//                kline = kline.replace("zhang_fu_ratio", zhangFuRatio);
//                kline = kline.replace("zhang_fu_zhi", zhangFuZhi);
//                klines.add(kline);
//                preLast = node.getLast();
//            }
//            log.info("res3={}", JSON.toJSON(klines));
//            return klines;
//        } catch (Exception ex) {
//            log.error("", ex);
//        }
//        return new ArrayList<>();
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

}
