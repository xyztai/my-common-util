package net.my.mapper;

import net.my.pojo.SpecialCarePoJo2;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyStockStrategyMapper {
    List<SpecialCarePoJo2> strategy_1();
    List<SpecialCarePoJo2> strategy_2();
    List<SpecialCarePoJo2> strategy_3();
    List<String> getLast5Days();
    List<SpecialCarePoJo2> strategy_5(@Param("calcDate") String calcDate);
}
