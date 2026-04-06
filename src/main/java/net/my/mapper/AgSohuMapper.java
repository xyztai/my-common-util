package net.my.mapper;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgSohuMapper {
    List<String> getStocks();
    List<String> getEtfs();
    List<String> getIndexs();
    String getMaxDateFromStock();
    String getMaxDateFromEtf();
}
