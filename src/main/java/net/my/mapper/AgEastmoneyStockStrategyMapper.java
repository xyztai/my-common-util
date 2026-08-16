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
    List<String> getLast120Days4Strategy5();
    int gen_strategy_5_default(@Param("calcDate") String calcDate);
    int gen_strategy_5(@Param("calcDate") String calcDate);
    List<SpecialCarePoJo2> strategy_5();
}
