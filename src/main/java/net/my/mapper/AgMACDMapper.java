package net.my.mapper;

import net.my.pojo.SpecialCarePoJo2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgMACDMapper {
    List<Double> getClosePrices();

    List<String> getCalcMACDDates4Stock();
    int genMACDData4Stock(@Param("calcDate") String calcDate);


    List<String> getCalcMACDDates4Etf();
    int genMACDData4Etf(@Param("calcDate") String calcDate);

}
