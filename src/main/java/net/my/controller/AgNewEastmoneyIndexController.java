package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.exception.CommonException;
import net.my.mapper.AgEastmoneyIndexMapper;
import net.my.pojo.BaseResponse;
import net.my.pojo.EastmoneyNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-eastmoney-index")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyIndexController {

    // demo: "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600276&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61";
    // fqt=1 表示前复权
    public static final String EASTMONEY_URL_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    public static final String EASTMONEY_URL_BEGIN_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=%s&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    @Autowired
    private AgEastmoneyIndexMapper agEastmoneyIndexMapper;

    @Autowired
    private RestTemplate restTemplate;

    @ApiOperation(value = "获取历史的cp数据（用来补历史数据）zqdm=1.000001", notes = "访问互联网接口获取数据")
    @GetMapping("/historyAll/{zqdm}")
    @Transactional
    public BaseResponse getHistoryDataOuter(@PathVariable("zqdm") String zqdm) {
        // 先判断是否存在，如果存在，则不允许插入，抛出异常， todo...
        String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);
        List<EastmoneyNode> eastmoneyNodeList = new ArrayList<>();
        try {
            log.info("/historyAll/{}, url={}", zqdm, url);
            String res = restTemplate.getForObject(url, String.class);
            List<String> klines = new ArrayList<>();
            if(!StringUtils.isEmpty(res)) {
                log.info("res={}", res);
                EtfEastmoneyRes eastmoneyRes = JSON.parseObject(res, EtfEastmoneyRes.class);
                log.info("eastmoneyRes={}", JSON.toJSONString(eastmoneyRes));

                // saveEastMoneyDatas
                if(eastmoneyRes == null || eastmoneyRes.getData() == null || CollectionUtils.isEmpty(eastmoneyRes.getData().getKlines())) {
                    throw new CommonException(401, "无数据");
                }

                klines = eastmoneyRes.getData().getKlines();
            } else {
                throw new CommonException(401, "无数据");
            }

            List<EastmoneyNode> nodes = new ArrayList<>();
            for(String item : klines) {
                String[] xxs = item.split(",");
                nodes.add(EastmoneyNode.builder().date(xxs[0]).stockCode(zqdm).infoRaw(item).build());
            }

            if(!CollectionUtils.isEmpty(nodes)) {
                eastmoneyNodeList.addAll(nodes);
            }
        } catch (Exception ex) {
            log.error("", ex);
            throw new CommonException(401, "获取数据异常");
        }

        int startNum = 0;
        int stepNum = 100;
        while(startNum < eastmoneyNodeList.size()) {
            List<EastmoneyNode> tmpNodes = eastmoneyNodeList.stream().skip(startNum).limit(stepNum).collect(Collectors.toList());
            log.info("tmpNodes.size={}", tmpNodes.size());
            agEastmoneyIndexMapper.saveIndexEastMoneyDatas(tmpNodes);
            startNum += stepNum;
        }

        log.info("阶段1-非99999数据-开始更新基础字段");
        // 更新基础字段
        agEastmoneyIndexMapper.updateIndexEastMoneyDatas();

        return BaseResponse.OK;
    }

    @Data
    public class EtfEastmoneyRes {
        private EtfEastmoneyPOJO data;
    }

    @Data
    public static class EtfEastmoneyPOJO {
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

