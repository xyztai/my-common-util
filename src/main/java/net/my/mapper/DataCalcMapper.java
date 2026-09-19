package net.my.mapper;

import net.my.pojo.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataCalcMapper {


    void delIndustryCalc(@Param("type") String type, @Param("time") String time);
    int saveIndustryCalc(AgIndustryCalcBO bo);
    List<String> getBuyInfo();
    List<String> getHistoryBuyRatio();
    List<AgIndustryCalcBO> getLastestIndustryData();
    int saveQqNodes(List<QqNode> qqNodes);
    QqNode getMaxQqNode(@Param("stockCode") String stockCode);
    List<SpecialCarePoJo> specialCare(@Param("time") String time);
    // hs300 数据
    List<HsStockPoJo> getHs300List();
    // hs300 参数
}
