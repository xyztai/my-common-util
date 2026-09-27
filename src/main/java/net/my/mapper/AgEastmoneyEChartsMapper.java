package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgEastmoneyEChartsMapper {
    // 从东财获取etf的数据
    String getBeginDate();
    int saveEcharts9ZhuanS(@Param("beginDate") String beginDate);
    int saveEcharts9ZhuanB(@Param("beginDate") String beginDate);
}
