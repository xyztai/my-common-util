package net.my.mapper;

import net.my.controller.AgNewController;
import net.my.pojo.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataCalcMapper {
    int deleteCP(String time);

    int deleteOneCP(@Param("time") String time, @Param("type") String type);

    int deleteDataCalcAfter(String time);

    int deleteDataCalc(String time);

    int insertCP(AgClosePriceBO bos);

    String queryMaxTime(String time);

    List<AgClosePriceBO> queryCP(String time);

    List<AgDataCalcBO> queryDataCalc(String time);

    List<AgDataCntBO> queryDataCnt();

    int queryCalcTimes();

    List<String> getUnCalcTimes();

    int insertDataCalc(@Param("time") String time);

//    List<AgOper> queryHardOper();

    List<AgOper> querySimpleOper();

    String getMaxTime();

    List<AgClosePriceBO> getExpectCP(@Param("time") String time, @Param("change") Double change);

    List<AgParaBO> queryPara();

    List<AgParaBO> queryMaxPara();

    int updatePara(AgParaBO bo);

    void deleteDailyPara();
    void saveDailyPara();

    List<AgExpectDataBO> getExpectData(@Param("type") String type);

    void deleteHistoryExpect();
    void insertHistoryExpect();

    void delIndustryCalc(@Param("type") String type, @Param("time") String time);
    int saveIndustryCalc(AgIndustryCalcBO bo);
    List<String> getBuyInfo();
    List<String> getHistoryBuyRatio();
    List<AgIndustryCalcBO> getLastestIndustryData();
    List<String> getFactor();
    void updateFactor(@Param("buyFactor") String buyFactor, @Param("sellFactor") String sellFactor);

    int saveQqNode(QqNode qqNode);
    int saveQqNodes(List<QqNode> qqNodes);
    QqNode getMaxQqNode(@Param("stockCode") String stockCode);
    List<SpecialCarePoJo> specialCare(@Param("time") String time);
    List<String> getLatestDates(@Param("days") Integer days);
    List<SpecialCarePoJo> selectExistedData(@Param("day") String day);
    List<SpecialCarePoJo> expectSpcialCare(@Param("swing") Double swing);
    int insertSpecialData(SpecialCarePoJo pojo);

    // hs300 数据
    List<Hs300PO> getHs300List();

    // hs300 参数
    List<Hs300Para> getHistoryParas();

    // 从东财获取所有的历史数据
    List<Hs300PO> getHs300FromEastmoneyList();

    int saveEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    EastmoneyNode getMaxEastMoneyNode(@Param("stockCode") String stockCode);
    EastmoneyNode getMaxEastMoneyNodeHasExpma(@Param("stockCode") String stockCode);
    List<EastmoneyNode> getEastMoneyNodes(@Param("stockCode") String stockCode);
    int updateEastMoneyDatas();
    int updateExpmaEastmoney(EastmoneyNode node);
    int saveEastmoneyNodeBuys(@Param("currDate") String currDate);
    List<String> getNeedCalcDates(@Param("startDate") String startDate, @Param("limitCnt") String limitCnt);
    List<String> getLatestDatesFromEastmoney(@Param("days") String days);
    int insertExpect99999();
    int deleteExpect99999();
    int delEastMoneyBuy99999();


    List<SpecialCarePoJo> queryEastmoneyExistedBuyData();
    List<SpecialCarePoJo> queryEastmoneyLast365();
    List<SpecialCarePoJo> queryEastmoneyLast30();
    List<SpecialCarePoJo2> queryEastmoneyVolSuddenlyRised();
}
