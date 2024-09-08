package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EastmoneyIndustryPOJO {
    private String key;
    private String title;
    private String href;
    private String target;
    private int order;
    private String groupKey;
    private List<EastmoneyIndustryPOJO> next;
}
