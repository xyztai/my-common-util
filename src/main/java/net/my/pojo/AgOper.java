package net.my.pojo;

import lombok.Data;

@Data
public class AgOper {
    private String time;
    private String name;
    private String operDir;
    private String buyOper;
    private String sellOper;
    private Double ratioC;
}
