package net.my.pojo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EastmoneyNode {
    private Long id;
    private String stockCode;
    private String date;
    private Double open;
    private Double last;
    private Double high;
    private Double low;
    private Double volume;
    private Double amount;
    private Double expma5;
    private Double expma10;
    private Double exchangeRaw;
    private String infoRaw;
}
