package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import net.my.pojo.HsStockPoJo;
import net.my.pojo.SpecialCarePoJo2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgQueryStockMapper {
    String getLimitDate();
    List<SpecialCarePoJo2> queryEastmoneyToday();
    List<SpecialCarePoJo2> queryEastmoneyLast30();
    List<SpecialCarePoJo2> queryIndexTop12In1Year();
    List<SpecialCarePoJo2> queryEastmoneyVolSuddenlyRisedTriple();
    List<SpecialCarePoJo2> query9ZhuanB();
    List<SpecialCarePoJo2> query9ZhuanS();
    List<SpecialCarePoJo2> queryEastmoneyLatestInfo();
    List<SpecialCarePoJo2> query9VolInLastest90Days();
    List<SpecialCarePoJo2> queryLatestRiseLimit();
    List<SpecialCarePoJo2> queryBigSwing();
    List<SpecialCarePoJo2> queryBigSwingAndLowestVol();
    List<SpecialCarePoJo2> queryBigSwingAndIn5LowestVol();
    List<SpecialCarePoJo2> queryDuoTouMA();
    List<SpecialCarePoJo2> queryUp5Lian();
    List<SpecialCarePoJo2> queryOnlyThem();
    List<SpecialCarePoJo2> jumpAndWait();
    List<SpecialCarePoJo2> MA20maSSP();
    List<SpecialCarePoJo2> considerAll();
    List<SpecialCarePoJo2> down5();
    List<SpecialCarePoJo2> getDailyCnt();
    List<SpecialCarePoJo2> get_000001_lowest();

    int saveRightData(List<SpecialCarePoJo2> eastmoneyNodes);
    String getMaxDate();
}
