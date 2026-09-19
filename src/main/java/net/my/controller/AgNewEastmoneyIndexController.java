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
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag-eastmoney-index")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgNewEastmoneyIndexController {

    @Autowired
    private AgEastmoneyIndexMapper agEastmoneyIndexMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AgNewSinaController agNewSinaController;


    @ApiOperation(value = "获取当天的数据（从sina来）zqdm=1.000001", notes = "访问互联网接口获取数据")
    @GetMapping("/historyAll-sina")
    @Transactional
    public BaseResponse getHistoryDataOuterSina() {
        List<EastmoneyNode> eastmoneyNodeList = new ArrayList<>();
        try {
            Map<String, String> sinaMap = agNewSinaController.getSinaDataForIndex();
            log.info("getSinaDataForIndex soHuMap = {}", JSON.toJSON(sinaMap));

            if(StringUtils.isEmpty(sinaMap)) {
                return BaseResponse.OK;
            }

            if(!CollectionUtils.isEmpty(sinaMap)) {
                for(Map.Entry<String, String> entry : sinaMap.entrySet()) {
                    String item = entry.getValue();
                    String[] xxs = item.split(",");
                    eastmoneyNodeList.add(EastmoneyNode.builder().date(xxs[0]).stockCode(entry.getKey()).infoRaw(item).build());
                }
            }
        } catch (Exception ex) {
            log.error("", ex);
//            throw new CommonException(401, "获取数据异常");
        }

        if(StringUtils.isEmpty(eastmoneyNodeList)) {
            return BaseResponse.OK;
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

