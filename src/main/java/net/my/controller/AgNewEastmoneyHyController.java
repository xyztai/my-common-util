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

import java.util.ArrayList;
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
//        String hyListUrl = EASTMONEY_URL_HY_LIST;
//        log.info("call hyListUrl={}", hyListUrl);
//        String res = restTemplate.getForObject(hyListUrl, String.class);
//        log.info("call hyListUrl={}, res={}", hyListUrl, res);
//        JSONObject jsonObject = JSONObject.parseObject(res);
//        JSONObject joData = jsonObject.getJSONObject("data");
//        JSONObject joDiff = joData.getJSONObject("diff");
//        for(Map.Entry<String, Object> item : joDiff.entrySet()){
//            log.info("{}", JSON.toJSONString(item.getValue()));
//        }

        String jsonArrayStr = "[{\"f12\":\"BK1031\",\"f14\":\"光伏设备\"},{\"f12\":\"BK1015\",\"f14\":\"能源金属\"},{\"f12\":\"BK1033\",\"f14\":\"电池\"},{\"f12\":\"BK1276\",\"f14\":\"油气开采Ⅱ\"},{\"f12\":\"BK0428\",\"f14\":\"电力\"},{\"f12\":\"BK1250\",\"f14\":\"煤炭开采\"},{\"f12\":\"BK1272\",\"f14\":\"旅游及景区\"},{\"f12\":\"BK0475\",\"f14\":\"银行Ⅱ\"},{\"f12\":\"BK1273\",\"f14\":\"体育Ⅱ\"},{\"f12\":\"BK1269\",\"f14\":\"旅游零售Ⅱ\"},{\"f12\":\"BK1271\",\"f14\":\"酒店餐饮\"},{\"f12\":\"BK0484\",\"f14\":\"贸易Ⅱ\"},{\"f12\":\"BK1262\",\"f14\":\"乘用车\"},{\"f12\":\"BK1266\",\"f14\":\"文娱用品\"},{\"f12\":\"BK0450\",\"f14\":\"航运港口\"},{\"f12\":\"BK1040\",\"f14\":\"中药Ⅱ\"},{\"f12\":\"BK0421\",\"f14\":\"铁路公路\"},{\"f12\":\"BK1277\",\"f14\":\"白酒Ⅱ\"},{\"f12\":\"BK1244\",\"f14\":\"小家电\"},{\"f12\":\"BK1028\",\"f14\":\"燃气Ⅱ\"},{\"f12\":\"BK0451\",\"f14\":\"房地产开发\"},{\"f12\":\"BK1241\",\"f14\":\"黑色家电\"},{\"f12\":\"BK1239\",\"f14\":\"白色家电\"},{\"f12\":\"BK1034\",\"f14\":\"其他电源设备Ⅱ\"},{\"f12\":\"BK0420\",\"f14\":\"航空机场\"},{\"f12\":\"BK1032\",\"f14\":\"风电设备\"},{\"f12\":\"BK0448\",\"f14\":\"通信设备\"},{\"f12\":\"BK0422\",\"f14\":\"物流\"},{\"f12\":\"BK0473\",\"f14\":\"证券Ⅱ\"},{\"f12\":\"BK1245\",\"f14\":\"照明设备Ⅱ\"},{\"f12\":\"BK1267\",\"f14\":\"造纸\"},{\"f12\":\"BK1228\",\"f14\":\"冶钢原料\"},{\"f12\":\"BK1256\",\"f14\":\"农产品加工\"},{\"f12\":\"BK0732\",\"f14\":\"贵金属\"},{\"f12\":\"BK1038\",\"f14\":\"光学光电子\"},{\"f12\":\"BK0474\",\"f14\":\"保险Ⅱ\"},{\"f12\":\"BK1027\",\"f14\":\"小金属\"},{\"f12\":\"BK1036\",\"f14\":\"半导体\"},{\"f12\":\"BK1264\",\"f14\":\"商用车\"},{\"f12\":\"BK0739\",\"f14\":\"工程机械\"},{\"f12\":\"BK0546\",\"f14\":\"玻璃玻纤\"},{\"f12\":\"BK1020\",\"f14\":\"非金属材料Ⅱ\"},{\"f12\":\"BK1248\",\"f14\":\"专业工程\"},{\"f12\":\"BK1042\",\"f14\":\"医药商业\"},{\"f12\":\"BK1044\",\"f14\":\"生物制品\"},{\"f12\":\"BK1259\",\"f14\":\"养殖业\"},{\"f12\":\"BK1252\",\"f14\":\"化妆品\"},{\"f12\":\"BK1281\",\"f14\":\"休闲食品\"},{\"f12\":\"BK1282\",\"f14\":\"饮料乳品\"},{\"f12\":\"BK1235\",\"f14\":\"环境治理\"},{\"f12\":\"BK1247\",\"f14\":\"基础建设\"},{\"f12\":\"BK1045\",\"f14\":\"房地产服务\"},{\"f12\":\"BK0734\",\"f14\":\"饰品\"},{\"f12\":\"BK1279\",\"f14\":\"非白酒\"},{\"f12\":\"BK1249\",\"f14\":\"焦炭Ⅱ\"},{\"f12\":\"BK1278\",\"f14\":\"调味发酵品Ⅱ\"},{\"f12\":\"BK1237\",\"f14\":\"自动化设备\"},{\"f12\":\"BK0459\",\"f14\":\"元件\"},{\"f12\":\"BK0482\",\"f14\":\"一般零售\"},{\"f12\":\"BK0465\",\"f14\":\"化学制药\"},{\"f12\":\"BK0424\",\"f14\":\"水泥\"},{\"f12\":\"BK1287\",\"f14\":\"工业金属\"},{\"f12\":\"BK1242\",\"f14\":\"家电零部件Ⅱ\"},{\"f12\":\"BK1251\",\"f14\":\"个护用品\"},{\"f12\":\"BK1039\",\"f14\":\"电子化学品Ⅱ\"},{\"f12\":\"BK1225\",\"f14\":\"服装家纺\"},{\"f12\":\"BK0457\",\"f14\":\"电网设备\"},{\"f12\":\"BK1236\",\"f14\":\"轨交设备Ⅱ\"},{\"f12\":\"BK0440\",\"f14\":\"家居用品\"},{\"f12\":\"BK1016\",\"f14\":\"汽车服务\"},{\"f12\":\"BK1265\",\"f14\":\"包装印刷\"},{\"f12\":\"BK0725\",\"f14\":\"装修装饰Ⅱ\"},{\"f12\":\"BK1223\",\"f14\":\"其他电子Ⅱ\"},{\"f12\":\"BK0910\",\"f14\":\"专用设备\"},{\"f12\":\"BK1280\",\"f14\":\"食品加工\"},{\"f12\":\"BK1253\",\"f14\":\"医疗美容\"},{\"f12\":\"BK0726\",\"f14\":\"工程咨询服务Ⅱ\"},{\"f12\":\"BK1227\",\"f14\":\"特钢Ⅱ\"},{\"f12\":\"BK1263\",\"f14\":\"摩托车及其他\"},{\"f12\":\"BK1037\",\"f14\":\"消费电子\"},{\"f12\":\"BK0454\",\"f14\":\"塑料\"},{\"f12\":\"BK0476\",\"f14\":\"装修建材\"},{\"f12\":\"BK1254\",\"f14\":\"动物保健Ⅱ\"},{\"f12\":\"BK1218\",\"f14\":\"出版\"},{\"f12\":\"BK0727\",\"f14\":\"医疗服务\"},{\"f12\":\"BK1234\",\"f14\":\"环保设备Ⅱ\"},{\"f12\":\"BK1270\",\"f14\":\"专业连锁Ⅱ\"},{\"f12\":\"BK0545\",\"f14\":\"通用设备\"},{\"f12\":\"BK1018\",\"f14\":\"橡胶\"},{\"f12\":\"BK1260\",\"f14\":\"渔业\"},{\"f12\":\"BK0481\",\"f14\":\"汽车零部件\"},{\"f12\":\"BK1258\",\"f14\":\"饲料\"},{\"f12\":\"BK1257\",\"f14\":\"农业综合Ⅱ\"},{\"f12\":\"BK1041\",\"f14\":\"医疗器械\"},{\"f12\":\"BK1255\",\"f14\":\"林业Ⅱ\"},{\"f12\":\"BK1030\",\"f14\":\"电机Ⅱ\"},{\"f12\":\"BK0539\",\"f14\":\"综合Ⅱ\"},{\"f12\":\"BK1288\",\"f14\":\"金属新材料\"},{\"f12\":\"BK1233\",\"f14\":\"军工电子Ⅱ\"},{\"f12\":\"BK1222\",\"f14\":\"影视院线\"}]";
        JSONArray jsonArray = new JSONArray(jsonArrayStr);

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String f12 = jsonObject.getString("f12");
            String f14 = jsonObject.getString("f14");
            log.info("getHyData f12={}, f14={}", f12, f14);
        }


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

