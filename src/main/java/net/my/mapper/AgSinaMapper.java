package net.my.mapper;

import net.my.controller.AgHistorySinaController;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgSinaMapper {
    List<String> getStocks();
    List<String> getEtfs();
    List<String> getIndexs();
    String getMaxDateFromStock();
    String getMaxDateFromEtf();

    int saveDataSina(List<AgHistorySinaController.DataSohu> dataSohuList);
}
