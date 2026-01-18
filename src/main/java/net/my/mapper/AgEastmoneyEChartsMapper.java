package net.my.mapper;

import net.my.pojo.EchartsPoJo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyEChartsMapper {
    // 从东财获取etf的数据
    List<EchartsPoJo> b69();
    String getBeginDate();
    int saveEcharts9Zhuan(@Param("beginDate") String beginDate);
}
