package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgParaBO {
    private int id;
    private String type;
    private String name;
    private Double sRatio;
    private Double bRatio;
}
