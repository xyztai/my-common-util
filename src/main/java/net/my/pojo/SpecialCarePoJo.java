package net.my.pojo;


import lombok.Data;

@Data
public class SpecialCarePoJo {
    private String stockCode;
    private String date;
    private Double last;
    private Double ratioB;
    private Double ratioS;
}
