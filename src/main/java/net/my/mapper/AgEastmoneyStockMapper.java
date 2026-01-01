package net.my.mapper;

import net.my.pojo.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyStockMapper {
    // hs300 数据
    List<HsStockPoJo> getHs300List();
    EastmoneyNode getMaxEastMoneyNode(@Param("stockCode") String stockCode);
    int saveEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    int updateEastMoneyDatas();
    int deleteExpect99999();
    int insertExpect99999();
    List<EastmoneyNode> getEastMoneyNodes(@Param("stockCode") String stockCode);
    EastmoneyNode getMaxEastMoneyNodeHasExpma(@Param("stockCode") String stockCode);
    int updateExpmaEastmoney(EastmoneyNode node);
    List<SpecialCarePoJo> queryEastmoneyToday();
    List<SpecialCarePoJo> queryEastmoneyLast30();
    List<SpecialCarePoJo2> queryEastmoneyVolSuddenlyRised();
    List<SpecialCarePoJo2> queryEastmoneyLatestInfo();
}
