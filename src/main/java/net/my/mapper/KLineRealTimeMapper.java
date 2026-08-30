package net.my.mapper;

import net.my.controller.KLineRealTimeController;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KLineRealTimeMapper {
    List<String> getAllQQStocks();

    int saveDataQQ(List<KLineRealTimeController.KLineRealTime> data);
}
