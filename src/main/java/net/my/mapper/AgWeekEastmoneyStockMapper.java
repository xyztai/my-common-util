package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import net.my.pojo.HsStockPoJo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgWeekEastmoneyStockMapper {
    // hs300 数据
    List<HsStockPoJo> getHs300List(@Param("targetDate") String targetDate);
    String getMaxEastMoneyNode(@Param("stockCode") String stockCode);
    int saveEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    int updateEastMoneyDatas();
    int genWeeklyData();
}
