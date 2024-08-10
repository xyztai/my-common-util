package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgDataCntBO {
    private String time;
    private Integer cntGap;
    private Integer cntCalc;
    private Integer cntCp;
    private String namesMiss;
}