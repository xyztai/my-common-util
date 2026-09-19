package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import net.my.pojo.HsStockPoJo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgHistoryMapper {
    String getStr(@Param("stockCode") String stockCode);
}
