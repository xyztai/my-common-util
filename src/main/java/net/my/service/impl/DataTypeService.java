package net.my.service.impl;

import net.my.mapper.AgMapper;
import net.my.pojo.AgDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataTypeService {
    @Autowired
    private AgMapper agMapper;

    public AgDataType getAgDataTypeByCode(String code) {
        String type = agMapper.getAgDataTypeByCode(code);
        switch (type) {
            case "1":
                return AgDataType.STOCK_CODE;
            case "2":
                return AgDataType.ETF;
            case "3":
                return AgDataType.INDEX;
            default:
                return null;
        }
    }
}
