package net.my.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TmpMapper {
    List<String> getDates(@Param("calcDate") String calcDate);
    void calcAvg(@Param("calcDate") String calcDate);
    List<String> getDatesVolMulti9(@Param("calcDate") String calcDate);
    void calcVolMulti9(@Param("calcDate") String calcDate);
}
