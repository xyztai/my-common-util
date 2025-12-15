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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-new-hs300")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNew300Controller {
    /**
     * 通过下面的3个接口，获取得到沪深300的列表，其中 f13 = 0 表示深圳；f13 = 1 表示上海
     *
     * https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104410682373325254_1765732657463&fs=b%3Abk0500%2Bf%3A!50&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f3&pn=1&pz=100&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1765732657486
     * https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104410682373325254_1765732657463&fs=b%3Abk0500%2Bf%3A!50&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f3&pn=2&pz=100&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1765732657486
     * https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104410682373325254_1765732657463&fs=b%3Abk0500%2Bf%3A!50&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f3&pn=3&pz=100&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1765732657486
     */

    // 复权（通达信默认为前复权）
    public static final String PROXY_FINANCE_QQ_URL_FORMAT_QFQ
            = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?_var=kline_dayqfq&param=%s,day,,,%d,qfq";
    // demo
//    public static final String URL_FORMAT_QFQ
//    = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?_var=kline_dayqfq&param=sz000001,day,,,2000,qfq";



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
        List<QqNode> qqNodeList = new ArrayList<>();
        List<Hs300PO> hs300List = dataCalcMapper.getHs300List();

        if(CollectionUtils.isEmpty(hs300List)) {
            return BaseResponse.OK;
        }

        Map<String, String> hs300Map = new LinkedHashMap<>();
        for(Hs300PO po : hs300List) {
            String key = "3_" + po.getStockName() + "-" + po.getStockCode();
            String value = 0 == po.getStockType() ? "sz" + po.getStockCode() : "sh" + po.getStockCode();
            hs300Map.put(key, value);
        }


        for(Map.Entry<String, String> entry : hs300Map.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(PROXY_FINANCE_QQ_URL_FORMAT_QFQ, zqdm, days);
            log.info("url: {}, zqdm: {}, days: {}", url, zqdm, days);
            String res = "";
            for(int i = 0; i < 200; i++) {
                try {
                    Thread.sleep(200);
                    log.info("try num={}, stockCode={}, url={}", i, entry.getKey(), url);
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

            try {
                QqRes qqRes = JSON.parseObject(res, QqRes.class);
                List<QqNode> tmpNodes = qqRes.getData().getNodes().stream()
                        .sorted(Comparator.comparing(QqNode::getDate)).collect(Collectors.toList());

                QqNode existsNode = dataCalcMapper.getMaxQqNode(entry.getKey());
                if(existsNode != null) {
                    tmpNodes = tmpNodes.stream()
                            .filter(f -> f.getDate().compareTo(existsNode.getDate()) > 0).collect(Collectors.toList());
                }

                if(!CollectionUtils.isEmpty(tmpNodes)) {
                    for(int i = 0; i < tmpNodes.size(); i++) {
                        QqNode currNode = tmpNodes.get(i);
                        currNode.setStockCode(entry.getKey());
                        if(0 == i) {
                            if(existsNode == null) {
                                currNode.setExpma5(currNode.getLast());
                                currNode.setExpma10(currNode.getLast());
                                currNode.setExpma20(currNode.getLast());
                                currNode.setExpma37(currNode.getLast());
                                currNode.setExpma60(currNode.getLast());
                            } else {
                                // private double calcExpma(double step, double lastValue, double cp) {
                                currNode.setExpma5(calcExpma(5.0, existsNode.getExpma5(), currNode.getLast()));
                                currNode.setExpma10(calcExpma(10.0, existsNode.getExpma10(), currNode.getLast()));
                                currNode.setExpma20(calcExpma(20.0, existsNode.getExpma20(), currNode.getLast()));
                                currNode.setExpma37(calcExpma(37.0, existsNode.getExpma37(), currNode.getLast()));
                                currNode.setExpma60(calcExpma(60.0, existsNode.getExpma60(), currNode.getLast()));
                            }
                        } else {
                            QqNode lastNode = tmpNodes.get(i - 1);
                            currNode.setExpma5(calcExpma(5.0, lastNode.getExpma5(), currNode.getLast()));
                            currNode.setExpma10(calcExpma(10.0, lastNode.getExpma10(), currNode.getLast()));
                            currNode.setExpma20(calcExpma(20.0, lastNode.getExpma20(), currNode.getLast()));
                            currNode.setExpma37(calcExpma(37.0, lastNode.getExpma37(), currNode.getLast()));
                            currNode.setExpma60(calcExpma(60.0, lastNode.getExpma60(), currNode.getLast()));
                        }
                    }
                    // 把最新的数据拿出来
                    qqNodeMap.put(entry.getKey(), tmpNodes.get(tmpNodes.size() - 1));
                    qqNodeList.addAll(tmpNodes);
                }
            } catch (Exception ex) {
                log.error("{}", ex);
            }
        }

        int startNum = 0;
        int stepNum = 100;
//        log.info("qqNodeList={}", JSON.toJSON(qqNodeList));
        while(startNum < qqNodeList.size()) {
            List<QqNode> tmpNodes = qqNodeList.stream().skip(startNum).limit(stepNum).collect(Collectors.toList());
            log.info("tmpNodes.size={}", tmpNodes.size());
            dataCalcMapper.saveQqNodes(tmpNodes);
            startNum += stepNum;
        }

//        qqNodeMap.values().forEach(qq -> dataCalcMapper.saveQqNode(qq));
        return RestGeneralResponse.of(qqNodeMap);
    }

    private double calcExpma(double step, double lastValue, double cp) {
        return (cp - lastValue) * 2.0 / (step + 1) + lastValue;
        // round((t.close_price - t3.`expma_5`)*2.0/(5.0+1) + t3.`expma_5`, 6)
//        BigDecimal bigDecimalLastValue = BigDecimal.valueOf(lastValue);
//        BigDecimal bigDecimalCp = BigDecimal.valueOf(cp);
//        return bigDecimalLastValue.add(
//                (bigDecimalCp.subtract(bigDecimalLastValue))
//                        .multiply(new BigDecimal(2.0))
//                        .divide(new BigDecimal(step + 1))
//        )
//                .doubleValue();
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
