package net.my.controller;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.my.interceptor.CurrentUser;
import net.my.interceptor.LoginRequired;
import net.my.mapper.DataCalcMapper;
import net.my.pojo.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ag")
@Slf4j
@Api(value = "ag", description = "ag接口")
public class AgController {

    static Map<String, String> map = new LinkedHashMap<>();
    static Map<String, String> eastmoneyMap = new LinkedHashMap<>();
    static Map<String, String> eastmoneyHbyqCMap = new LinkedHashMap<>();

    static Map<String, String> eastmoneyIndustryMap = new LinkedHashMap<>();

    static {
        map.put("sz50", "sh000016");
        map.put("szzs", "sh000001");
        map.put("hs300", "sz399300");
        map.put("szcz", "sz399001");
        map.put("kc50", "sh000688");
        map.put("zz1000", "sh000852");
        map.put("zz2000", "sz399303");
        map.put("bz50", "bj899050");
        map.put("hskjzs", "hkHSTECH");
        map.put("nsdk100", "usNDX");
        // map.put("zq", "sh600030"); // 中信证券
        // map.put("ysjs", "sh000819");
        // map.put("gfcy", "sh601012"); // 隆基
        // map.put("ktjg", "sz930875");
        // map.put("rjzs", "sh012637");
        // map.put("hbyqC", "sh007844");
        map.put("ljln", "sh601012"); // 隆基绿能
        map.put("ndsd", "sz300750"); // 宁德时代
        map.put("ymkd", "sh603259"); // 药明康德
        map.put("tqly", "sz002466"); // 天齐锂业


        eastmoneyMap.put("zq", "1.512880"); // 证券
        eastmoneyMap.put("ysjs", "1.000819"); // 有色金属
        eastmoneyMap.put("gfcy", "2.931151"); // 光伏产业
        eastmoneyMap.put("ktjg", "2.930875"); // 空天军工
        eastmoneyMap.put("rjzs", "2.H30202"); // 软件指数

        eastmoneyHbyqCMap.put("hbyqC", "007844.OF");
    }

    /*
    sz50	上证50
    szzs	上证指数
    hs300	沪深300
    szcz	深证成指
    kc50	科创50
    zz1000	中证1000
    zz2000	中证2000
    bz50	北证50
    hskjzs	恒生科技
    zq	    证券
    ysjs	有色金属
    gfcy	光伏产业
    ktjg	空天军工
    rjzs	软件指数
    hbyqC	华宝油气C
    nsdk100	纳斯达克100
     */

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataCalcMapper dataCalc;

    @Autowired
    private RestTemplate restTemplate;

    public static final String URL_FORMAT = "https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?_var=kline_dayqfq&param=%s,day,,,%d,qfq";
    public static final String EASTMONEY_URL_FORMAT =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&klt=101&fqt=1&lmt=%d";
    // public static final String EASTMONEY_URL_FORMAT_SUFFIX = "&end=20500000&iscca=1&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6%2Cf7%2Cf8&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61%2Cf62%2Cf63%2Cf64&ut=f057cbcbce2a86e2866ab8877db1d059&forcect=1";
    public static final String EASTMONEY_URL_FORMAT_SUFFIX = "&end=20500000&fields1=f1,f2,f3,f4,f5,f6,f7,f8&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64";
    public static final String EASTMONEY_URL_FORMAT_HBYQC = "https://datacenter.eastmoney.com//securities/api/data/get?type=RPT_F10_FUND_PERNAV&sty=SECURITY_CODE,END_DATE,PER_NAV&filter=(SECUCODE=\"%s\")&source=HSF10&client=APP&p=1&ps=%d&sr=-1&st=END_DATE";
    public static final String[] TYPES = {"sh000001"};

    public static final int HISTORY_DAYS = 320;

    public static final int DAYS_CNT = 1;

    // 将远程的json文件拉取到本地
    public static void main(String[] args) {
        String urlString = "https://quote.eastmoney.com/center/api/sidemenu.json"; // 获取行业基础信息的json
        String filePath = "sidemenu.json"; // 本地文件路径

        try (InputStream inputStream = new URL(urlString).openStream();
             FileOutputStream outputStream = new FileOutputStream(filePath)
        ) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("JSON文件已保存至: " + filePath);
            // 读取刚刚存的文件数据
            if(true) {
                String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
                Gson gson = new Gson();
                List<EastmoneyIndustryPOJO> list = JSON.parseArray(jsonString, EastmoneyIndustryPOJO.class);
                System.out.println(list);
                for(EastmoneyIndustryPOJO l1 : list) {
                    if("沪深京板块".equals(l1.getTitle()) && !CollectionUtils.isEmpty(l1.getNext())) {
                        List<EastmoneyIndustryPOJO> l2List = l1.getNext();
                        for(EastmoneyIndustryPOJO l2 : l2List) {
                            if("行业板块".equals(l2.getTitle()) && !CollectionUtils.isEmpty(l2.getNext())) {
                                List<EastmoneyIndustryPOJO> l3List = l2.getNext();
                                for(EastmoneyIndustryPOJO l3 : l3List) {
                                    if(Strings.isNotEmpty(l3.getKey()) && l3.getKey().split("-").length > 1)
                                        System.out.println(String.format("%s: %s", l3.getTitle(), l3.getKey().split("-")[1]));
                                }
                            }
                        }
                    }
                }
            }

            // 读取resources下的文件数据
            if(true) {
                File file = ResourceUtils.getFile("sidemenu.json");
                System.out.println(file.toPath().toAbsolutePath().toString());
                String jsonString = org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                List<EastmoneyIndustryPOJO> list = JSON.parseArray(jsonString, EastmoneyIndustryPOJO.class);
                System.out.println(list);
                for(EastmoneyIndustryPOJO l1 : list) {
                    if("沪深京板块".equals(l1.getTitle()) && !CollectionUtils.isEmpty(l1.getNext())) {
                        List<EastmoneyIndustryPOJO> l2List = l1.getNext();
                        for(EastmoneyIndustryPOJO l2 : l2List) {
                            if("行业板块".equals(l2.getTitle()) && !CollectionUtils.isEmpty(l2.getNext())) {
                                List<EastmoneyIndustryPOJO> l3List = l2.getNext();
                                for(EastmoneyIndustryPOJO l3 : l3List) {
                                    if(Strings.isNotEmpty(l3.getKey()) && l3.getKey().split("-").length > 1)
                                        System.out.println(String.format("%s: %s", l3.getTitle(), l3.getKey().split("-")[1]));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/industry")
    public BaseResponse getIndustryHistoryData() {
        Map<String, Object> resMap = new LinkedHashMap<>();
        List<String> buyInfos = dataCalc.getBuyInfo();
        resMap.put("todayBuyInfosSize", CollectionUtils.isEmpty(buyInfos) ? 0 : buyInfos.size());
        resMap.put("todayBuyInfos", buyInfos);
        List<String> historyBuyRatioInfos = dataCalc.getHistoryBuyRatio();
        resMap.put("historyBuyRatioInfosSize", CollectionUtils.isEmpty(historyBuyRatioInfos) ? 0 : historyBuyRatioInfos.size());
        resMap.put("historyBuyRatioInfos", historyBuyRatioInfos);
        return RestGeneralResponse.of(resMap);
    }


    @GetMapping("/industry/{days}")
    public BaseResponse getIndustryHistoryData(@PathVariable("days") Integer days) {
        List<AgIndustryCalcBO> agIndustryCalcBOList = new ArrayList<>();
        try {
            eastmoneyIndustryMap.clear();
            Resource resource = applicationContext.getResource("classpath:sidemenu.json");
            // log.info("file-path: {}", resource.getFile().getAbsoluteFile());
            InputStream inputStream = resource.getInputStream();
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String jsonString = writer.toString();
            List<EastmoneyIndustryPOJO> l1List = JSON.parseArray(jsonString, EastmoneyIndustryPOJO.class);
            for(EastmoneyIndustryPOJO l1 : l1List) {
                if("沪深京板块".equals(l1.getTitle()) && !CollectionUtils.isEmpty(l1.getNext())) {
                    List<EastmoneyIndustryPOJO> l2List = l1.getNext();
                    for(EastmoneyIndustryPOJO l2 : l2List) {
                        if("行业板块".equals(l2.getTitle()) && !CollectionUtils.isEmpty(l2.getNext())) {
                            List<EastmoneyIndustryPOJO> l3List = l2.getNext();
                            for(EastmoneyIndustryPOJO l3 : l3List) {
                                if(Strings.isNotEmpty(l3.getKey()) && l3.getKey().split("-").length > 1) {
                                    eastmoneyIndustryMap.put(l3.getTitle(), l3.getKey().split("-")[1]);
                                    log.info("{}: {}", l3.getTitle(), l3.getKey().split("-")[1]);
                                }
                            }
                        }
                    }
                }
            }

            for(Map.Entry<String, String> entry : eastmoneyIndustryMap.entrySet()) {
                String zqdm = entry.getValue();
                String url = String.format(EASTMONEY_URL_FORMAT, zqdm, days) + EASTMONEY_URL_FORMAT_SUFFIX;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
                headers.set("Referer", "https://wap.eastmoney.com/");
                headers.set("Origin", "https://wap.eastmoney.com");
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");

                String res = restTemplate.getForObject(url, String.class, headers);
                assert res != null;
                log.info("url: {}, zqdm: {}, res: {}", url, zqdm, res);
                String data = JSON.parseObject(res).getString("data");
                String dayData = JSON.parseObject(data).getString("klines");
                List<String> list = JSON.parseObject(dayData, List.class);
                for(String obj : list) {
                    String[] tmp = obj.split(",");
                    String time = tmp[0];
                    if("2023-01-03".compareTo(time) >= 0)
                        continue;
                    // Double oP = Double.parseDouble (((String)innerList.get(1)).replaceAll("\"", ""));
                    Double cP = Double.parseDouble (tmp[2]);
                    // Double hP = Double.parseDouble (((String)innerList.get(3)).replaceAll("\"", ""));
                    // Double lP = Double.parseDouble (((String)innerList.get(4)).replaceAll("\"", ""));
                    AgIndustryCalcBO bo = AgIndustryCalcBO.builder().name(entry.getKey()).type(zqdm).closePrice(cP).time(time).build();
                    agIndustryCalcBOList.add(bo);
                }
            }
            agIndustryCalcBOList = agIndustryCalcBOList.stream().sorted(Comparator.comparing(AgIndustryCalcBO::getName).thenComparing(AgIndustryCalcBO::getTime)).collect(Collectors.toList());
            // expma_5: round((t.close_price - t3.`expma_5`)*2.0/(5.0+1) + t3.`expma_5`, 6) clac_expma_5
            // expma_37: round((t.close_price - t3.`expma_37`)*2.0/(37.0+1) + t3.`expma_37`, 6) clac_expma_37
            List<String> names = agIndustryCalcBOList.stream().map(AgIndustryCalcBO::getName).distinct().collect(Collectors.toList());
            List<AgIndustryCalcBO> agIndustryCalcBOs = dataCalc.getLastestIndustryData();
            for(String name : names) {
                List<AgIndustryCalcBO> tmpList = agIndustryCalcBOList.stream().filter(f -> name.equals(f.getName()))
                        .sorted(Comparator.comparing(AgIndustryCalcBO::getTime)).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(tmpList)) {
                    if(!CollectionUtils.isEmpty(agIndustryCalcBOs) &&
                            agIndustryCalcBOs.stream().anyMatch(f -> name.equals(f.getName()))) {
                        AgIndustryCalcBO tmpBo = agIndustryCalcBOs.stream().filter(f -> name.equals(f.getName())).findFirst().get();
                        tmpList = tmpList.stream().filter(f -> f.getTime().compareTo(tmpBo.getTime()) > 0).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(tmpList) && tmpBo != null) {
                            Double cp = tmpList.get(0).getClosePrice();
                            Double expma5 = (cp - tmpBo.getExpma5()) * 2.0 / (5.0 + 1) + tmpBo.getExpma5();
                            tmpList.get(0).setExpma5(getScaleDouble(expma5, 6));
                            Double expma37 = (cp - tmpBo.getExpma37()) * 2.0 / (37.0 + 1) + tmpBo.getExpma37();
                            tmpList.get(0).setExpma37(getScaleDouble(expma37, 6));
                            Double sRation = expma5 / expma37;
                            tmpList.get(0).setSRatio(getScaleDouble(sRation, 6));
                            Double bRation = expma37 / expma5;
                            tmpList.get(0).setBRatio(getScaleDouble(bRation, 6));
                        }
                    } else {
                        tmpList.get(0).setExpma5(tmpList.get(0).getClosePrice());
                        tmpList.get(0).setExpma37(tmpList.get(0).getClosePrice());
                        tmpList.get(0).setSRatio(1.0);
                        tmpList.get(0).setBRatio(1.0);
                    }
                    if(!CollectionUtils.isEmpty(tmpList)) {
                        for(int i = 1; i < tmpList.size(); i++) {
                            Double cp = tmpList.get(i).getClosePrice();
                            Double expma5 = (cp - tmpList.get(i - 1).getExpma5()) * 2.0 / (5.0 + 1) + tmpList.get(i - 1).getExpma5();
                            tmpList.get(i).setExpma5(getScaleDouble(expma5, 6));
                            Double expma37 = (cp - tmpList.get(i - 1).getExpma37()) * 2.0 / (37.0 + 1) + tmpList.get(i - 1).getExpma37();
                            tmpList.get(i).setExpma37(getScaleDouble(expma37, 6));
                            Double sRation = expma5 / expma37;
                            tmpList.get(i).setSRatio(getScaleDouble(sRation, 6));
                            Double bRation = expma37 / expma5;
                            tmpList.get(i).setBRatio(getScaleDouble(bRation, 6));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("", ex.getMessage(), ex);
            ex.printStackTrace();
        }
        agIndustryCalcBOList.forEach(f -> dataCalc.delIndustryCalc(f.getType(), f.getTime()));
        agIndustryCalcBOList.forEach(f -> dataCalc.saveIndustryCalc(f));
        Map<String, Object> resMap = new LinkedHashMap<>();
        resMap.put("insertSize", agIndustryCalcBOList.size());
        List<String> buyInfos = dataCalc.getBuyInfo();
        resMap.put("todayBuyInfos", buyInfos);
        List<String> historyBuyRatioInfos = dataCalc.getHistoryBuyRatio();
        resMap.put("historyBuyRatioInfos", historyBuyRatioInfos);
        return RestGeneralResponse.of(resMap);
    }

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
        for(Map.Entry<String, String> entry : map.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(URL_FORMAT, zqdm, days);
            String res = restTemplate.getForObject(url, String.class);
            assert res != null;
            res = res.replaceAll("kline_dayqfq=", "");
            log.info("res: {}", res);
            String data = JSON.parseObject(res).getString("data");
            String typeData = JSON.parseObject(data).getString(zqdm);
            String dayData = JSON.parseObject(typeData).getString("day");
            List list = JSON.parseObject(dayData, List.class);
            if(list == null) {
                dayData = JSON.parseObject(typeData).getString("qfqday");
                list = JSON.parseObject(dayData, List.class);
            }
            for(Object obj : list) {
                log.info(JSON.toJSONString(obj));
                List innerList = JSON.parseObject(JSON.toJSONString(obj), List.class);
                String time = ((String)innerList.get(0)).replaceAll("\"", "");
                Double oP = Double.parseDouble (((String)innerList.get(1)).replaceAll("\"", ""));
                Double cP = Double.parseDouble (((String)innerList.get(2)).replaceAll("\"", ""));
                Double hP = Double.parseDouble (((String)innerList.get(3)).replaceAll("\"", ""));
                Double lP = Double.parseDouble (((String)innerList.get(4)).replaceAll("\"", ""));
                if(agClosePriceDTOs.stream().map(AgClosePriceDTO::getTime).collect(Collectors.toList()).contains(time)) {
                    AgClosePriceDTO dto = agClosePriceDTOs.stream().filter(f -> time.equals(f.getTime())).findAny().get();
                    dto.setValue(entry.getKey(), cP);
                } else {
                    AgClosePriceDTO dto = new AgClosePriceDTO();
                    dto.setTime(time);
                    dto.setValue(entry.getKey(), cP);
                    agClosePriceDTOs.add(dto);
                }
            }
        }

        for(Map.Entry<String, String> entry : eastmoneyMap.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(EASTMONEY_URL_FORMAT, zqdm, days) + EASTMONEY_URL_FORMAT_SUFFIX;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.set("Referer", "https://wap.eastmoney.com/");
            headers.set("Origin", "https://wap.eastmoney.com");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");

            String res = restTemplate.getForObject(url, String.class, headers);
            assert res != null;
            log.info("url: {}, zqdm: {}, res: {}", url, zqdm, res);
            String data = JSON.parseObject(res).getString("data");
            String dayData = JSON.parseObject(data).getString("klines");
            List<String> list = JSON.parseObject(dayData, List.class);
            for(String obj : list) {
                String[] tmp = obj.split(",");
                String time = tmp[0];
                // Double oP = Double.parseDouble (((String)innerList.get(1)).replaceAll("\"", ""));
                Double cP = Double.parseDouble (tmp[2]);
                // Double hP = Double.parseDouble (((String)innerList.get(3)).replaceAll("\"", ""));
                // Double lP = Double.parseDouble (((String)innerList.get(4)).replaceAll("\"", ""));
                if(agClosePriceDTOs.stream().map(AgClosePriceDTO::getTime).collect(Collectors.toList()).contains(time)) {
                    AgClosePriceDTO dto = agClosePriceDTOs.stream().filter(f -> time.equals(f.getTime())).findAny().get();
                    dto.setValue(entry.getKey(), cP);
                } else {
                    AgClosePriceDTO dto = new AgClosePriceDTO();
                    dto.setTime(time);
                    dto.setValue(entry.getKey(), cP);
                    agClosePriceDTOs.add(dto);
                }
            }
        }

        for(Map.Entry<String, String> entry : eastmoneyHbyqCMap.entrySet()) {
            String zqdm = entry.getValue();
            String url = String.format(EASTMONEY_URL_FORMAT_HBYQC, zqdm, days);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.set("Referer", "https://wap.eastmoney.com/");
            headers.set("Origin", "https://wap.eastmoney.com");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");

            String res = restTemplate.getForObject(url, String.class, headers);
            assert res != null;
            log.info("url: {}, zqdm: {}, res: {}", url, zqdm, res);
            String result = JSON.parseObject(res).getString("result");
            String dayData = JSON.parseObject(result).getString("data");
            List list = JSON.parseObject(dayData, List.class);
            for(Object obj : list) {
                String time = JSON.parseObject(obj.toString()).getString("END_DATE").split(" ")[0];
                // Double oP = Double.parseDouble (((String)innerList.get(1)).replaceAll("\"", ""));
                Double cP = Double.parseDouble (JSON.parseObject(obj.toString()).getString("PER_NAV"));
                // Double hP = Double.parseDouble (((String)innerList.get(3)).replaceAll("\"", ""));
                // Double lP = Double.parseDouble (((String)innerList.get(4)).replaceAll("\"", ""));
                if(agClosePriceDTOs.stream().map(AgClosePriceDTO::getTime).collect(Collectors.toList()).contains(time)) {
                    AgClosePriceDTO dto = agClosePriceDTOs.stream().filter(f -> time.equals(f.getTime())).findAny().get();
                    dto.setValue(entry.getKey(), cP);
                } else {
                    AgClosePriceDTO dto = new AgClosePriceDTO();
                    dto.setTime(time);
                    dto.setValue(entry.getKey(), cP);
                    agClosePriceDTOs.add(dto);
                }
            }
        }

        agClosePriceDTOs = agClosePriceDTOs.stream().filter(f -> "2023-01-03".compareTo(f.getTime()) < 0).collect(Collectors.toList());
        log.info("agClosePriceDTOs :{}", agClosePriceDTOs);
        String timeMin = agClosePriceDTOs.stream().map(AgClosePriceDTO::getTime).sorted().findFirst().get();
        log.info("timeMin: {}", timeMin);
        agClosePriceDTOs.stream().filter(f -> !timeMin.equals(f.getTime())).forEach(this::onlyAddData);
        agClosePriceDTOs.stream().filter(f -> timeMin.equals(f.getTime())).forEach(this::addData);

        return RestGeneralResponse.of(agClosePriceDTOs);
    }


    @ApiOperation(value = "删除cp数据", notes = "删除指定日期的cp数据")
    @ApiImplicitParam(name = "time", value = "日期", required = true, dataType = "String")
    @LoginRequired
    @DeleteMapping("/remove/{time}")
    @Transactional
    public BaseResponse remove(@CurrentUser UserBase userBase, @PathVariable("time") String time) {
        log.info("userBase: {}", userBase);
        log.info("remove-time:{}", time);
        dataCalc.deleteCP(time);
        calc(time);
        return RestGeneralResponse.of("删除完成");
    }

    @ApiOperation(value = "获取预期结果数据", notes = "根据策略，得到指定类型的交易结果")
    @ApiImplicitParam(name = "type", value = "类型", required = true, dataType = "String")
    @GetMapping("/getExpectData/{type}")
    public BaseResponse getExpectData(@PathVariable("type") String type) {
        log.info("getExpectData: type = {}", type);
        List<AgExpectDataBO> bos = dataCalc.getExpectData(type);
        if(CollectionUtils.isEmpty(bos)) {
            return RestGeneralResponse.of("无期望数据，无法预测");
        }
        Long moneyInit = 0L;
        Double initPrice = bos.get(0).getClosePrice();
        Double numberInit = moneyInit/initPrice;
        Map<String, String> resMap = new LinkedHashMap<>();
        Long addCangTotal = 0L;
        Long subCangTotal = 0L;
        Double bRatioAns = 0.0;
        Double sRatioAns = 0.0;
        Double bActionMax = 0.0;
        Double sActionMax = 0.0;
        bos.forEach(f -> log.info(JSON.toJSONString(f)));
        for(int i = 1; i < bos.size(); i++) {
            AgExpectDataBO tmp = bos.get(i);
            String operStr = "nop......";
            if(tmp.getBAction() != null && bRatioAns < tmp.getBRatioAns() && bActionMax < tmp.getBAction()) {
                numberInit += tmp.getBAction() / tmp.getClosePrice();
                addCangTotal += tmp.getBAction().longValue();
                sRatioAns = 0.0;
                sActionMax = 0.0;
                bRatioAns = tmp.getBRatioAns();
                bActionMax = tmp.getBAction();
                operStr = "+c " + tmp.getBAction().longValue();
            }
            if(tmp.getSAction() != null && moneyInit >= 100 && sRatioAns < tmp.getSRatioAns() && sActionMax < tmp.getSAction()) {
                bRatioAns = 0.0;
                bActionMax = 0.0;
                sRatioAns = tmp.getSRatioAns();
                sActionMax = tmp.getSAction();
                // > 1，意味着是实际金额
                if(tmp.getSAction() > 1) {
                    numberInit -= tmp.getSAction() / tmp.getClosePrice();
                    subCangTotal += tmp.getSAction().longValue();
                    operStr = "-c " + tmp.getBAction().longValue();
                } else {
                    // <= 1，意味着是比例
                    subCangTotal += (long)(numberInit * tmp.getSAction() * tmp.getClosePrice());
                    numberInit = numberInit * (1 - tmp.getSAction());
                    operStr = "-c " + tmp.getSAction();
                }
            }
            moneyInit = (long)(numberInit * tmp.getClosePrice());
            resMap.put(tmp.getTime(), operStr + ", moneyNow " + moneyInit + ",  gain " + (moneyInit + subCangTotal - addCangTotal));
        }

        return RestGeneralResponse.of(resMap);
    }

    @ApiOperation(value = "查询当前使用的参数值")
    @GetMapping("/para")
    public BaseResponse queryPara() {
        return RestGeneralResponse.of(dataCalc.queryPara());
    }

    @ApiOperation(value = "更新当前使用的参数值", notes = "根据策略，得到指定类型的交易结果")
    @PostMapping("/updatePara")
    @Transactional
    public BaseResponse updatePara() {
        log.info("updatePara begin");
        /*
        List<AgParaBO> paras = dataCalc.queryPara();
        List<AgParaBO> maxParas = dataCalc.queryMaxPara();
        for(AgParaBO bo : paras) {
            Optional<AgParaBO> tmp = maxParas.stream().filter(f -> f.getType().equals(bo.getType()) *//*&& f.getBRatio() > bo.getBRatio()*//*).findFirst();
            tmp.ifPresent(f -> bo.setBRatio(f.getBRatio()));
            tmp = maxParas.stream().filter(f -> f.getType().equals(bo.getType()) *//*&& f.getSRatio() > bo.getSRatio()*//*).findFirst();
            tmp.ifPresent(f -> bo.setSRatio(f.getSRatio()));
            dataCalc.updatePara(bo);
        }
        */
        List<AgParaBO> maxParas = dataCalc.queryMaxPara();
        maxParas.forEach(f -> dataCalc.updatePara(f));
        log.info("updatePara end");
        return queryPara();
    }

    /**
     * 只计算往前推250天的数据，根据这250天的数据计算得到当天的参数，再重新计算预估的expma值
     * @return
     */
    @ApiOperation(value = "生成历史上逐日的参数数据，以及预估的expma值", notes = "只计算往前推250天的数据，根据这250天的数据计算得到当天的参数，再重新计算预估的expma值")
    @PostMapping("/genDailyParaAndHistoryExpect")
    @Transactional
    public BaseResponse genDailyParaAndHistoryExpect() {
        log.info("genDailyParaAndHistoryExpect begin");
        dataCalc.deleteDailyPara();
        dataCalc.saveDailyPara();
        dataCalc.deleteHistoryExpect();
        dataCalc.insertHistoryExpect();
        log.info("genDailyParaAndHistoryExpect end");
        return BaseResponse.OK;
    }


    @ApiOperation(value = "获取cp数据", notes = "根据日期获取数据")
    @ApiImplicitParam(name = "time", value = "日期", required = true, dataType = "String")
    @GetMapping("/data/cp/{time}")
    public BaseResponse queryCP(@PathVariable("time") String time) {
        List<AgClosePriceBO> bos = dataCalc.queryCP(time);

        Map<String, Double> retMap = new LinkedHashMap<>();
        if(!CollectionUtils.isEmpty(bos)) {
            bos.forEach(
                    f -> retMap.put(f.getName(), f.getClosePrice())
            );
        }

        return RestGeneralResponse.of(retMap);
    }

    @ApiOperation(value = "统计每天有多少expma数据", notes = "结果按照日期倒排")
    @GetMapping("/data/cnt")
    public BaseResponse queryDataCnt() {
        List<AgDataCntBO> bos = dataCalc.queryDataCnt();

        return RestGeneralResponse.of(bos);
    }

    @ApiOperation(value = "获取expma数据", notes = "根据日期获取数据")
    @ApiImplicitParam(name = "time", value = "日期", required = true, dataType = "String")
    @GetMapping("/data/calc/{time}")
    public BaseResponse queryDataCalc(@PathVariable("time") String time) {
        List<AgDataCalcBO> bos = dataCalc.queryDataCalc(time);
        bos = bos.stream().sorted(Comparator.comparingDouble(AgDataCalcBO::getMaxCompare).reversed()).collect(Collectors.toList());

        List<String> retList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(bos)) {
            retList = bos.stream().map(m -> String.format("%s %s: expma5=%.3f, expma37=%.3f, maxCompare=%.4f%s sRatio=%.4f%s%s(para=%.3f, pre=%.4f), bRatio=%.4f%s%s(para=%.3f, pre=%.4f)"
                    , m.getDirection()
                    , m.getName(), m.getExpma5(), m.getExpma37()
                    , m.getMaxCompare(), " %"
                    , m.getSRatio(), m.getSRatio()>=1 && m.getSRatio() >= m.getSRatioPre() ? " ↑↑↑ " : ""
                    , m.getSRatio()>=1 && m.getSRatio() >= m.getSRatioPara() ? " *** " : ""
                    , m.getSRatioPara(), m.getSRatioPre()
                    , m.getBRatio(), m.getBRatio()>=1 && m.getBRatio() >= m.getBRatioPre() ? " ↑↑↑ " : ""
                    , m.getBRatio()>=1 && m.getBRatio() >= m.getBRatioPara() ? " *** " : ""
                    , m.getBRatioPara(), m.getBRatioPre())).collect(Collectors.toList());
        }

        return RestGeneralResponse.of(retList);
    }

    @ApiOperation(value = "获取接下来的操作，每个操作只会执行一次", notes = "需要给出波动值")
    @ApiImplicitParam(name = "change", value = "波动值", required = true, dataType = "Double")
    @GetMapping("/expect/hard2/{change}")
    @Transactional
    public BaseResponse expectHard2(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectHard: time={}, change={}", time, change);
        if(dataCalc.getMaxTime().compareTo(time) >= 0) {
            return RestGeneralResponse.of(String.format("已存在日期大于或等于 %s 的数据，无需预测~", time));
        }

        List<AgClosePriceBO> agClosePriceBOList = dataCalc.getExpectCP(time, change);
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        dataCalc.insertCP(f);
                    }
            );

            calc(time);
            BaseResponse response = queryHardOper2();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    @ApiOperation(value = "获取接下来的操作，只要严于上一次的操作参数就会再执行，即使操作与上一次操作相同", notes = "需要给出波动值")
    @ApiImplicitParam(name = "change", value = "波动值", required = true, dataType = "Double")
    @GetMapping("/expect/hard/{change}")
    @Transactional
    public BaseResponse expectHard(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectHard: time={}, change={}", time, change);
        if(dataCalc.getMaxTime().compareTo(time) >= 0) {
            return RestGeneralResponse.of(String.format("已存在日期大于或等于 %s 的数据，无需预测~", time));
        }

        List<AgClosePriceBO> agClosePriceBOList = dataCalc.getExpectCP(time, change);
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        dataCalc.insertCP(f);
                    }
            );

            calc(time);
            BaseResponse response = queryHardOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    @ApiOperation(value = "获取接下来的操作，高于给定的参数值，就会执行", notes = "需要给出波动值")
    @ApiImplicitParam(name = "change", value = "波动值", required = true, dataType = "Double")
    @GetMapping("/expect/simple/{change}")
    @Transactional
    public BaseResponse expectSimple(@PathVariable("change") Double change) {
        String time = "9999-99-99";
        log.info("expectSimple: time={}, change={}", time, change);
        if(dataCalc.getMaxTime().compareTo(time) >= 0) {
            return RestGeneralResponse.of(String.format("已存在日期大于或等于 %s 的数据，无需预测~", time));
        }

        List<AgClosePriceBO> agClosePriceBOList = dataCalc.getExpectCP(time, change);
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        dataCalc.insertCP(f);
                    }
            );

            calc(time);
            BaseResponse response = querySimpleOper();

            // 测试结束，就删除掉
            dataCalc.deleteCP(time);
            dataCalc.deleteDataCalc(time);
            return response;
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    private void onlyAddData(AgClosePriceDTO agClosePriceDTO) {
        List<AgClosePriceBO> agClosePriceBOList = agClosePriceDTO.toBO();
        agClosePriceBOList = agClosePriceBOList.stream().filter(f -> !StringUtils.isEmpty(f.getClosePrice())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        // 先删后插
                        dataCalc.deleteOneCP(f.getTime(), f.getType());
                        dataCalc.insertCP(f);
                    }
            );
        }
    }

    @ApiOperation(value = "手动添加/更新cp值")
    @ApiImplicitParam(name = "agClosePriceDTO", value = "cp值", required = true, dataType = "net.my.pojo.AgClosePriceDTO")
    @PostMapping("/add")
    @Transactional
    public BaseResponse addData(@RequestBody AgClosePriceDTO agClosePriceDTO) {
        log.info("agClosePriceDTO:{}", JSON.toJSONString(agClosePriceDTO));
        List<AgClosePriceBO> agClosePriceBOList = agClosePriceDTO.toBO();
        agClosePriceBOList = agClosePriceBOList.stream().filter(f -> !StringUtils.isEmpty(f.getClosePrice())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(agClosePriceBOList)) {
            agClosePriceBOList.forEach(
                    f -> {
                        // 先删后插
                        dataCalc.deleteOneCP(f.getTime(), f.getType());
                        dataCalc.insertCP(f);
                    }
            );

            calc(agClosePriceDTO.getTime());
            return RestGeneralResponse.of("数据添加完成");
        } else {
            return RestGeneralResponse.of("无数据");
        }
    }

    private void calc(String time) {
        // 删掉比插入的数据时间大的所有数据
        dataCalc.deleteDataCalcAfter(time);

        // 重新计算后续所有的数据
        List<String> times = dataCalc.getUnCalcTimes();

        log.info("times:{}", times);
        times.stream().sorted().forEach(dataCalc::insertDataCalc);
    }

    @GetMapping("/oper/hard")
    public BaseResponse queryHardOper() {
        log.info("queryHardOper.");
        List<AgOper> opers = dataCalc.querySimpleOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }
        opers.forEach(f -> log.info("queryHardOper dataCalc.querySimpleOper() {}", JSON.toJSONString(f)));

        opers = opers.stream().sorted(Comparator.comparing(AgOper::getName).thenComparing(AgOper::getTime)).collect(Collectors.toList());
        List<AgOper> res = new ArrayList<>();
        AgOper pre = opers.get(0);
        for(int i = 1; i < opers.size(); i++) {
            AgOper curr = opers.get(i);
            if(pre != null
                    && curr.getName().equals(pre.getName())
                    && curr.getOperDir().equals(pre.getOperDir())) {
                if(pre.getRatioC() < curr.getRatioC()) {
                    res.add(curr);
                    pre = curr;
                }
            } else {
                pre = curr;
                res.add(curr);
            }
        }

        if(CollectionUtils.isEmpty(res)) {
            return RestGeneralResponse.of("无操作");
        }

        res = res.stream().sorted(Comparator.comparing(AgOper::getTime).reversed().thenComparing(AgOper::getOperDir)).collect(Collectors.toList());

        res.forEach(f -> log.info("queryHardOper res {}", JSON.toJSONString(f)));

        return RestGeneralResponse.of(makeMap(opers));
    }

    @GetMapping("/oper/hard2")
    public BaseResponse queryHardOper2() {
        log.info("queryHardOper.");
        List<AgOper> opers = dataCalc.querySimpleOper();

        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }
        opers.forEach(f -> log.info("queryHardOper2 dataCalc.querySimpleOper() {}", JSON.toJSONString(f)));

        opers = opers.stream().sorted(Comparator.comparing(AgOper::getName).thenComparing(AgOper::getTime)).collect(Collectors.toList());
        List<AgOper> res = new ArrayList<>();
        AgOper pre = opers.get(0);
        for(int i = 1; i < opers.size(); i++) {
            AgOper curr = opers.get(i);
            if(pre != null
                    && curr.getName().equals(pre.getName())
                    && curr.getOperDir().equals(pre.getOperDir())) {
                if(pre.getRatioC() < curr.getRatioC()) {
                    if(!(curr.getBuyOper() + curr.getSellOper()).equals(pre.getBuyOper() + pre.getSellOper())) {
                        res.add(curr);
                    }
                    pre = curr;
                }
            } else {
                pre = curr;
                res.add(curr);
            }
        }

        if(CollectionUtils.isEmpty(res)) {
            return RestGeneralResponse.of("无操作");
        }

        res = res.stream().sorted(Comparator.comparing(AgOper::getTime).reversed().thenComparing(AgOper::getOperDir)).collect(Collectors.toList());

        res.forEach(f -> log.info("queryHardOper2 res {}", JSON.toJSONString(f)));
        return RestGeneralResponse.of(makeMap(res));
    }

    @GetMapping("/oper/simple")
    public BaseResponse querySimpleOper() {
        log.info("querySimpleOper.");
        List<AgOper> opers = dataCalc.querySimpleOper();
        if(CollectionUtils.isEmpty(opers)) {
            return RestGeneralResponse.of("无操作");
        }

        return RestGeneralResponse.of(makeMap(opers));
    }

    private Map<String, Map<String, List>> makeMap(List<AgOper> opers) {
        Map<String, Map<String, List>> retMap = new LinkedHashMap<>();
        if(CollectionUtils.isEmpty(opers)) {
            return retMap;
        }

        Comparator<String> comp = (String::compareTo);
        List<String> times = opers.stream().map(AgOper::getTime).distinct().sorted(comp.reversed()).collect(Collectors.toList());
        if(!times.contains("9999-99-99")) {
            retMap.put("预期操作", null);
        }
        log.info("opers: {}", opers);
        for(String time : times) {
            Map<String, List> timeMap = new LinkedHashMap<>();
            List<String> buyList = opers.stream().filter(f -> time.equals(f.getTime()) && "buy".equals(f.getOperDir()))
                    .map(m -> m.getName() + ": " + m.getBuyOper().replaceAll("buy", "买")).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(buyList)) {
                timeMap.put("买:", buyList);
            }

            List<String> sellList = opers.stream().filter(f -> time.equals(f.getTime()) && "sell".equals(f.getOperDir()))
                    .map(m -> m.getName() + ": " + m.getSellOper().replaceAll("sell", "卖")).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(sellList)) {
                timeMap.put("卖:", sellList);
            }

            if(!CollectionUtils.isEmpty(timeMap)) {
                if("9999-99-99".equals(time)) {
                    retMap.put("预期操作", timeMap);
                } else {
                    retMap.put(time, timeMap);
                }
            }
        }
        return retMap;
    }

}
