package net.my.mapper;

import net.my.pojo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataCalcMapper {
    int deleteCP(String time);

    int deleteOneCP(@Param("time") String time, @Param("type") String type);

    int deleteDataCalcAfter(String time);

    int deleteDataCalc(String time);

    int insertCP(AgClosePriceBO bos);

    List<AgClosePriceBO> queryCP(String time);

    List<AgDataCalcBO> queryDataCalc(String time);

    List<AgDataCntBO> queryDataCnt();

    int queryCalcTimes();

    List<String> getUnCalcTimes();

    int insertDataCalc(@Param("time") String time);

    List<AgOper> queryHardOper();

    List<AgOper> querySimpleOper();

    String getMaxTime();

    List<AgClosePriceBO> getExpectCP(@Param("time") String time, @Param("change") Double change);

    List<AgParaBO> queryPara();

    List<AgParaBO> queryMaxPara();

    int updatePara(AgParaBO bo);
}
