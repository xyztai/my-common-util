package net.my.mapper;

import net.my.pojo.SpecialCarePoJo2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgCCIEastmoneyStockMapper {
    List<String> getCalcCCIDates4Stock();
    int genCCIData4Stock(@Param("calcDate") String calcDate);


    List<String> getCalcCCIDates4Etf();
    int genCCIData4Etf(@Param("calcDate") String calcDate);

    List<SpecialCarePoJo2> considerCCIAndVol();
}
