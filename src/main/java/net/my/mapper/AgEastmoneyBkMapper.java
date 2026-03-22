package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgEastmoneyBkMapper {
    int saveBkInfo(@Param("bkCode") String bkCode, @Param("bkName") String bkName);
}
