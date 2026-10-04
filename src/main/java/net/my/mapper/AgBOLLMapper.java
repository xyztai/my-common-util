package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgBOLLMapper {
    List<String> getCalcBOLLDates4Stock();
    int genBOLLData4Stock(@Param("calcDate") String calcDate);


    List<String> getCalcBOLLDates4Etf();
    int genBOLLData4Etf(@Param("calcDate") String calcDate);

}
