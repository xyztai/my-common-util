package net.my.mapper;

import net.my.controller.AgNewSinaHistoryController;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgSohuMapper {
    List<String> getStocks();
    List<String> getEtfs();
    List<String> getIndexs();
    String getMaxDateFromStock();
    String getMaxDateFromEtf();

    int saveDataSohu(List<AgNewSinaHistoryController.DataSohu> dataSohuList);
}
