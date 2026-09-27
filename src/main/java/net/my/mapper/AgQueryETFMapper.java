package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import net.my.pojo.HsStockPoJo;
import net.my.pojo.SpecialCarePoJo2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgQueryETFMapper {
    List<SpecialCarePoJo2> queryEtfEastmoneyToday();
    List<SpecialCarePoJo2> queryEtfEastmoneyLast60();
    List<SpecialCarePoJo2> queryEtfEastmoneyVolSuddenlyRised();
    List<SpecialCarePoJo2> queryEtf9ZhuanS();
    List<SpecialCarePoJo2> queryEtfEastmoneyLatestInfo();
    List<SpecialCarePoJo2> queryEtfLastest90Days();

    List<SpecialCarePoJo2> queryEtfChgTop3();
    List<SpecialCarePoJo2> queryEtfChgTop3History();
}
