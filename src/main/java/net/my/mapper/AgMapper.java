package net.my.mapper;

import net.my.pojo.EastmoneyNode;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgMapper {
    int saveNodeDatas(@Param("nodeTableName") String nodeTableName, @Param("eastmoneyNodes") List<EastmoneyNode> eastmoneyNodes);
    int updateNodeDatas(@Param("nodeTableName") String nodeTableName);
    String getAgDataTypeByCode(@Param("code") String code);

    List<String> getCodes(@Param("codeTableName") String codeTableName);

}
