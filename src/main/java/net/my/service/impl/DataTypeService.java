package net.my.service.impl;

import net.my.mapper.AgHistoryMapper;
import net.my.pojo.AgDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataTypeService {
    @Autowired
    private AgHistoryMapper agHistoryMapper;

    public AgDataType getAgDataTypeByCode(String code) {
        String type = agHistoryMapper.getAgDataTypeByCode(code);
        switch (type) {
            case "1":
                return AgDataType.Stock_Code;
            case "2":
                return AgDataType.ETF;
            case "3":
                return AgDataType.Index;
            default:
                return null;
        }
    }
}
