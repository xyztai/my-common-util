package net.my.controller;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.my.cache.MyCaffeineCache;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        eastmoneyMap.put("bj", "0.399997"); // 中证白酒
        eastmoneyMap.put("mt", "0.399998"); // 中证煤炭
        eastmoneyMap.put("yycx", "2.931484"); // 医药创新

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
    private MyCaffeineCache myCaffeineCache;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataCalcMapper dataCalcMapper;

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
        List<String> buyInfos = dataCalcMapper.getBuyInfo();
        resMap.put("todayBuyInfosSize", CollectionUtils.isEmpty(buyInfos) ? 0 : buyInfos.size());
        resMap.put("todayBuyInfos", buyInfos);
        List<String> historyBuyRatioInfos = dataCalcMapper.getHistoryBuyRatio();
        resMap.put("historyBuyRatioInfosSize", CollectionUtils.isEmpty(historyBuyRatioInfos) ? 0 : historyBuyRatioInfos.size());
        resMap.put("historyBuyRatioInfos", historyBuyRatioInfos);
        return RestGeneralResponse.of(resMap);
    }


    @GetMapping("/industry/{days}")
    public BaseResponse getIndustryHistoryData(@PathVariable("days") Integer days) {
        List<AgIndustryCalcBO> todoList = new ArrayList<>();
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

                log.info("url: {}, zqdm: {}, headers: {}", url, zqdm, JSON.toJSON(headers));
                String res = "";
                for(int i = 0; i < 200; i++) {
                    try {
                        Thread.sleep(200);
                        log.info("try num={}, zqdm={}, url={}", i, zqdm, url);
                        res = restTemplate.getForObject(url, String.class, headers);
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
            List<AgIndustryCalcBO> agIndustryCalcBOs = dataCalcMapper.getLastestIndustryData();
            for(String name : names) {
                List<AgIndustryCalcBO> tmpList = agIndustryCalcBOList.stream().filter(f -> name.equals(f.getName()))
                        .sorted(Comparator.comparing(AgIndustryCalcBO::getTime)).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(tmpList)) {
                    if(!CollectionUtils.isEmpty(agIndustryCalcBOs) &&
                            agIndustryCalcBOs.stream().anyMatch(f -> name.equals(f.getName()))) {
                        AgIndustryCalcBO tmpBo = agIndustryCalcBOs.stream().filter(f -> name.equals(f.getName())).findFirst().get();
                        log.info("tmpBo: {}", tmpBo);
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
                    todoList.addAll(tmpList);
                }
            }
        } catch (Exception ex) {
            log.error("", ex.getMessage(), ex);
            ex.printStackTrace();
        }
        todoList.forEach(f -> dataCalcMapper.delIndustryCalc(f.getType(), f.getTime()));
        todoList.forEach(f -> dataCalcMapper.saveIndustryCalc(f));
        Map<String, Object> resMap = new LinkedHashMap<>();
        resMap.put("insertSize", todoList.size());
        List<String> buyInfos = dataCalcMapper.getBuyInfo();
        resMap.put("todayBuyInfos", buyInfos);
        List<String> historyBuyRatioInfos = dataCalcMapper.getHistoryBuyRatio();
        resMap.put("historyBuyRatioInfos", historyBuyRatioInfos);
        return RestGeneralResponse.of(resMap);
    }

    private Double getScaleDouble(Double dou, int scale) {
        BigDecimal bd = new BigDecimal(dou);
        return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }





}
