package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyIndexMapper {
    String getStr(@Param("stockCode") String stockCode);
    int saveIndexEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    int updateIndexEastMoneyDatas();
}
