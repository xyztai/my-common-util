package net.my.mapper;

import net.my.pojo.AgClosePriceBO;
import net.my.pojo.AgOper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataCalcMapper {
    int deleteCP(String time);

    int deleteOneCP(@Param("time") String time, @Param("type") String type);

    int deleteDataCalcAfter(String time);

    int insertCP(AgClosePriceBO bos);

    int queryCalcTimes();

    int insertDataCalc();

    List<AgOper> queryOper();
}
