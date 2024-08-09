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
}
