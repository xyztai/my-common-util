package net.my.pojo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EastmoneyTmpCalc {
    private String methodName;
    private String date;
    private String stockCode;
    private String ratioB;
}
