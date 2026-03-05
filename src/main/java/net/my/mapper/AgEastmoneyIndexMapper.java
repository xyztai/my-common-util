package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyIndexMapper {
    int saveIndexEastMoneyDatas(List<EastmoneyNode> eastmoneyNodes);
    int updateIndexEastMoneyDatas();
}
