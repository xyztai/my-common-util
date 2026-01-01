package net.my.mapper;

import net.my.pojo.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyEtfMapper {
    // 从东财获取etf的数据
    List<HsStockPoJo> getEtfList();
    EastmoneyNode getEtfMaxEastMoneyNode(@Param("stockCode") String stockCode);
    int saveEtfEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    int updateEtfEastMoneyDatas();
    int deleteEtfExpect99999();
    int insertEtfExpect99999();
    List<EastmoneyNode> getEtfEastMoneyNodes(@Param("stockCode") String stockCode);
    EastmoneyNode getEtfMaxEastMoneyNodeHasExpma(@Param("stockCode") String stockCode);
    int updateEtfExpmaEastmoney(EastmoneyNode node);
    List<SpecialCarePoJo> queryEtfEastmoneyToday();
    List<SpecialCarePoJo> queryEtfEastmoneyLast60();
    List<SpecialCarePoJo2> queryEtfEastmoneyVolSuddenlyRised();
}
