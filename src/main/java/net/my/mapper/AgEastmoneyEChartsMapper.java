package net.my.mapper;

import net.my.pojo.EchartsPoJo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyEChartsMapper {
    // 从东财获取etf的数据
    List<EchartsPoJo> b69();
}
