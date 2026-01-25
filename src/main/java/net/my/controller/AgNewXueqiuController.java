package net.my.controller;

import com.alibaba.fastjson2.JSON;
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
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36");
            headers.add("cookie", "xq_a_token=ac2a78f80e88c34c3386da6345e0f7562548dd15; xqat=ac2a78f80e88c34c3386da6345e0f7562548dd15; xq_r_token=d20e14f6a702a89477d31c11c7ee913bbb801fb9; xq_id_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1aWQiOi0xLCJpc3MiOiJ1YyIsImV4cCI6MTc3MDY4NjM1OCwiY3RtIjoxNzY5Mjk1NDIwNDg3LCJjaWQiOiJkOWQwbjRBWnVwIn0.efHg5tkN7CHHJEypbfmGSecG6VWJdFjkhDfNb-of3vrrLFoabnqb_yI3uc_yz-9alrcspozv_igXhRUjm2758q6xgpdDX-Au3tqI6BzmanArSzKCMqE7GWtVypFYsJFwcpSp727ZMhQZPdxxC82cvhs7Cxw7DlwAGANid7TV_9WUM6l6F2QcottzpVtv4ZwPHog9HcA0c9WgS7BiOEbNBleiGDqZnMdZ4WB3rPE9oAACS7Zuus39xS-hUWNbksP8Oy0XXvrF8vDN-SyO1M3h1xu1Wt9avSg8TZq04iNgWI3EYR4HByPJCR_eQZK77l57wysCIso1UA1-7nwpR3-nvQ; cookiesu=941769295423940; u=941769295423940; device_id=aa2aeecca150c7466123b2bdaee081fb; ssxmod_itna=1-iqjxy70Qitvxhx_Oxeq7KiK9Mx3wDBP011WDuxiK08D6GxBbRkei=AGrKed7dner92qeKeBD0H=iDnqD8jDQeDvD28DDCDDqUqOEadbR_d_0=k9mvrMqZa4nt_K10uELTs==Q2wwqeGLDY=DCdxQYSiD4b3Dt4DIDAYDDxDWDYExxGUtDG=D7rbQymT3xi3DbrRexitDKwbDA4GIbp2asDDB830F=ERjDDNcRRDDY_K8b6BDH6wFuQh_wY4ox0tTDBd80tdo2=M6u4n_wzSTN_rDzd1Dtwut410FsEbrMlW_KBYQKDm60B3GDtrYxrB3d7DOoxeAsonDol=HhKxQxOhQnADYnN7BipOdDD3CmpRnX6QGGD4ccUyPXgx_DfhmQGemmw7G/mDqGx4BP4CPqjDCWDCAqznDYzIqrwPCW1f5VrmPYD; ssxmod_itna2=1-iqjxy70Qitvxhx_Oxeq7KiK9Mx3wDBP011WDuxiK08D6GxBbRkei=AGrKed7dner92qeKeGDDPYqDGoxYDBj7oGazKhfK4d=iqDsUYQpF_Eo0l1inErFwOGNqsu=pcuR2OP2t6RrSFdQFAOGEFYhv/qwDOdhpSS=S4fXU8Qb427o5w7Df7ojKVkoQAwjeluuvYloqIfvonmKFukoKcm5SFA9Vn_t87nNqYQXbZ8D/7haytuHSnYgYbBvX8ChFqqwCDaXfI8GxlqkMmfQXRMoRRLActjvUft_r2QhUYGX1ImuRjTM0mQhx/uMBueDYdb0V_WjnvaDH7A4qS_KBw_3wwCrIFqLz4IF0s7cXS7=0bPVuyIRqZu70cm0027q9YGCUbuYmtB4p3u8zTegq4xPP=cmIoLnKhUmkiuDmP=jwxRcOCuov_xbcsWqhnK=zy/lbZSFODB9_HiiqPPm1nwil7h8aI4YO_0CQaeu5niECF_18oCsqx4dK5KrdHnSW44FL8ebT5sRvRY0lW5az4_8nDVW5PnQ5Dq3nblkYvD1WqAld18Qi47vnhihmG3cl81NUTeOwvR7QZnDKpEAR4FDvnei8FT7o212Qc9xwWAvki=BqqzXFuTlRGAuvnm1ULxHqoOh27MCPea9nmCxO/nE7Pnpwf8dIwk7jUhV4X00UNPPVcHfId8LPLhF2MfI6Tb9tQhWlto29/5gD1/eCut/tXT70flTfCqCM1AiEblYm54uKp6wQK2tjGYrZO8S0RreMqiGKG2vD504fStBq7mE/wHfK=jo8T4pRGHQQFhfbjBYU4R54x8pB4eDhpBowR=oeRh1A5ldNdT0GAe3yR6Q4eD");

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
            res = response.getBody();
            XueqiuRes xueqiuRes = JSON.parseObject(res, XueqiuRes.class);
            log.info("xueqiuRes={}", JSON.toJSON(xueqiuRes));

        } catch (Exception ex) {
            log.error("{}", ex);
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
    public class XueqiuRes{
        private Integer error_code;
        private String error_description;
        private XueqiuData data;
    }

    @Data
    public class XueqiuData{
        private List<XueqiuNode> item;
    }

    @Data
    public class XueqiuNode{
        private List<Double> klineValues;
    }

}
