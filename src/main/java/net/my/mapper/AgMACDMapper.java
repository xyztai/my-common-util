package net.my.mapper;

import net.my.pojo.RawPO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgMACDMapper {
    List<String> getAllCodesStock();
    List<RawPO> getClosePricesBatchStock(@Param("batchCodes") List<String> batchCodes);

    List<String> getAllCodesEtf();
    List<RawPO> getClosePricesBatchEtf(@Param("batchCodes") List<String> batchCodes);
}
