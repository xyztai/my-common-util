package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgMapper {
    int saveNodeDatas(@Param("tableName") String tableName, @Param("eastmoneyNodes") List<EastmoneyNode> eastmoneyNodes);
    int updateNodeDatas(@Param("tableName") String tableName);
    String getAgDataTypeByCode(@Param("code") String code);

    List<String> getCodes(@Param("tableName") String tableName);

}
