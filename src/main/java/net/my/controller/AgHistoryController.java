package net.my.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.my.exception.CommonException;
import net.my.mapper.AgHistoryMapper;
import net.my.mapper.AgMapper;
import net.my.pojo.AgDataType;
import net.my.pojo.BaseResponse;
import net.my.pojo.EastmoneyNode;
import net.my.service.impl.DataTypeService;
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

/**
 * 根据表 t_eastmoney_node_str 提取全量的历史数据，但是只能逐个取解析，因为量太大了，接口就会被封
 * 这个接口非常重要
 */
@RestController
@RequestMapping("/ag-history")
@Slf4j
@Api(value = "ag-history", description = "ag计算接口")
public class AgHistoryController {

    // demo: "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600276&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61";
    // fqt=1 表示前复权
    public static final String EASTMONEY_URL_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=0&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    public static final String EASTMONEY_URL_BEGIN_FORMAT_QFQ =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&beg=%s&end=20500101&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    @Autowired
    private DataTypeService dataTypeService;

    @Autowired
    private AgMapper agMapper;

    @Autowired
    private AgHistoryMapper agHistoryMapper;

    @Autowired
    private RestTemplate restTemplate;

    @ApiOperation(value = "获取历史的cp数据（用来补历史数据）zqdm=1.000001", notes = "访问互联网接口获取数据")
    @GetMapping("/historyAll/{zqdm}")
    @Transactional
    public BaseResponse historyAll(@PathVariable("zqdm") String zqdm) {
        log.info("historyAll zqdm={}", zqdm);

        // 先判断是否存在，如果存在，则直接取表中的数据，否则就需要访问网络接口，获取数据
        String url = String.format(EASTMONEY_URL_FORMAT_QFQ, zqdm);
        List<EastmoneyNode> eastmoneyNodeList = new ArrayList<>();

        AgDataType agDataType = dataTypeService.getAgDataTypeByCode(zqdm);
        if(agDataType == null) {
            throw new CommonException(401, "该代码不存在");
        }

        try {
            String res = "";
            String tableValue = agHistoryMapper.getStr(zqdm);
            if(!StringUtils.isEmpty(res)) {
                // 直接用表中数据
                res = tableValue;
                log.info("agHistoryMapper.getStr res={}", res);
            } else {
                log.info("/historyAll/{}, url={}", zqdm, url);
                // 需要访问网络获取
                res = restTemplate.getForObject(url, String.class);
                log.info("restTemplate.getForObject res={}", res);
            }

            List<String> klines = new ArrayList<>();
            if(StringUtils.isEmpty(res)) {
                throw new CommonException(401, "无数据");
            }

            // 解析结果
            EastmoneyRes eastmoneyRes = JSON.parseObject(res, EastmoneyRes.class);
            log.info("eastmoneyRes={}", JSON.toJSONString(eastmoneyRes));

            // saveEastMoneyDatas
            if(eastmoneyRes == null || eastmoneyRes.getData() == null || CollectionUtils.isEmpty(eastmoneyRes.getData().getKlines())) {
                throw new CommonException(401, "无数据");
            }

            klines = eastmoneyRes.getData().getKlines();

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
            log.info("插入infoRaw字段 tmpNodes.size={}", tmpNodes.size());
            agMapper.saveNodeDatas(agDataType.getNodeTableName(), tmpNodes);
            startNum += stepNum;
        }

        log.info("根据infoRaw字段,更新基础字段");
        // 更新基础字段
        agMapper.updateNodeDatas(agDataType.getNodeTableName());

        return BaseResponse.OK;
    }

    @Data
    public class EastmoneyRes {
        private EastmoneyPOJO data;
    }

    @Data
    public static class EastmoneyPOJO {
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
