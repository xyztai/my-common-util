package net.my.pojo;


import lombok.Data;

@Data
public class QqNode{
    private String stockCode;
    private String date;
    private Double open;
    private Double last;
    private Double high;
    private Double low;
    private Double volume;
    private Double amount;
    private Double exchangeRaw;
}
