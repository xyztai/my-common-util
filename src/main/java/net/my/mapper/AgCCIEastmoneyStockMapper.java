package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgCCIEastmoneyStockMapper {
    List<String> getCalcCCIDates();
    int genCCIData(@Param("calcDate") String calcDate);
}
