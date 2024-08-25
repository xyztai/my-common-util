package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgDataCalcBO {
    private int id;
    private String time;
    private String type;
    private String name;
    private Double expma5;
    private Double expma37;
    private Double sRatioPara;
    private Double sRatio;
    private Double sRatioPre;
    private Double bRatioPara;
    private Double bRatio;
    private Double bRatioPre;

    public Double getMaxCompare() {
        return Math.max(sRatio > 1 ? sRatio/sRatioPara : 0, bRatio > 1 ? bRatio/bRatioPara : 0);
    }

    public String getDirection() {
        return sRatio/sRatioPara > bRatio/bRatioPara ? "S" : "B";
    }

}
