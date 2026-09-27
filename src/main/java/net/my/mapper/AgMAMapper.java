package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgMAMapper {
    List<String> getCalcMADates4Stock();
    int genMAData4Stock(@Param("calcDate") String calcDate);


    List<String> getCalcMADates4Etf();
    int genMAData4Etf(@Param("calcDate") String calcDate);

}
