package net.my.mapper;

import net.my.pojo.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyStockMapper {
    String getLimitDate();
    // hs300 数据
    List<HsStockPoJo> getHs300List();
    EastmoneyNode getMaxEastMoneyNode(@Param("stockCode") String stockCode);
    int saveEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    int updateEastMoneyDatas();
    int deleteExpect99999();
    int insertExpect99999();
    List<EastmoneyNode> getAllNeedUpdateEastMoneyNodes();
    List<EastmoneyNode> getEastMoneyNodes(@Param("stockCode") String stockCode);
    List<EastmoneyNode> getAllMaxEastMoneyNodeHasExpma();
    EastmoneyNode getMaxEastMoneyNodeHasExpma(@Param("stockCode") String stockCode);
    int updateExpmaEastmoney(EastmoneyNode node);
    int batchUpdateExpmaEastmoney(List<EastmoneyNode> node);
    List<SpecialCarePoJo2> queryEastmoneyToday();
    List<SpecialCarePoJo2> queryEastmoneyLast30();
    List<SpecialCarePoJo2> queryEastmoneyVolSuddenlyRisedTriple();
    List<SpecialCarePoJo2> query9ZhuanB();
    List<SpecialCarePoJo2> query9ZhuanS();
    List<SpecialCarePoJo2> queryEastmoneyLatestInfo();
    List<SpecialCarePoJo2> queryLastest9Zhuan9Vol();
    List<SpecialCarePoJo2> query9VolInLastest90Days();
    List<SpecialCarePoJo2> queryAvg60();
    List<SpecialCarePoJo2> queryLatestRiseLimit();
    List<SpecialCarePoJo2> queryBigSwing();
    List<SpecialCarePoJo2> queryBigSwingAndLowestVol();
    List<SpecialCarePoJo2> queryDuoTou();
    List<SpecialCarePoJo2> queryDuoTouMA();
    List<SpecialCarePoJo2> queryUp5Lian();
    List<SpecialCarePoJo2> queryOnlyThem();
    List<SpecialCarePoJo2> jumpAndWait();
    List<SpecialCarePoJo2> MA20maSSP();
    List<SpecialCarePoJo2> considerAll();
    List<String> getCalcDatesFromDuoTou();
    int genDuoTou(@Param("calcDate") String calcDate);
    List<String> getCalcDatesFromMA();
    int genMA(@Param("calcDate") String calcDate);
}
