package net.my.pojo;


import lombok.Data;

@Data
public class SpecialCarePoJo2 {
    private String stockCode;
    private String date;
    private String last;
    private String ratioB;

    public EastmoneyTmpCalc toPO(String methodName) {
        return EastmoneyTmpCalc.builder()
                .methodName(methodName)
                .date(this.date)
                .stockCode(this.stockCode)
                .ratioB(this.ratioB)
                .build();
    }
}
