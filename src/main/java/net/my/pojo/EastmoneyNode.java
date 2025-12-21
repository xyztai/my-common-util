package net.my.pojo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EastmoneyNode {
    private Integer id;
    private String date;
    private String stockCode;
    private Double open;
    private Double last;
    private Double high;
    private Double low;
    private Double volume;
    private Double amount;
    private Double exchangeRaw;
    private String infoRaw;
    private Double expma5;
    private Double expma10;
}
