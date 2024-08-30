package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgExpectDataBO {
    private int id;
    private String time;
    private String type;
    private String name;
    private Double closePrice;
    private Double bAction;
    private Double sAction;
}
