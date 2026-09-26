package net.my.mapper;

import net.my.pojo.SpecialCarePoJo2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgMAEastmoneyStockMapper {
    List<String> getCalcMADates4Stock();
    int genMAData4Stock(@Param("calcDate") String calcDate);


    List<String> getCalcMADates4Etf();
    int genMAData4Etf(@Param("calcDate") String calcDate);

}
