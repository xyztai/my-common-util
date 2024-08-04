package net.my.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgClosePriceBO {
    private String time;
    private String type;
    private String closePrice;
}
