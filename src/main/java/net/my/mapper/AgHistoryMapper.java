package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgHistoryMapper {
    String getStr(@Param("stockCode") String stockCode);
}
