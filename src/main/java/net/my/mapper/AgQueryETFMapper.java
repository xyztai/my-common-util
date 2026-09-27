package net.my.mapper;

import net.my.pojo.SpecialCarePoJo2;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgQueryETFMapper {
    List<SpecialCarePoJo2> queryEtfChgTop3();
    List<SpecialCarePoJo2> queryEtfChgTop3History();
}
