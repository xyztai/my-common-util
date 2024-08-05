package net.my.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgClosePriceDTO {
    private String time;
    private Double sz50CP;
    private Double szzsCP;
    private Double hs300CP;
    private Double szczCP;
    private Double kc50CP;
    private Double zz1000CP;
    private Double zz2000CP;
    private Double bz50CP;
    private Double hskjzsCP;
    private Double zqCP;
    private Double ysjsCP;
    private Double gfcyCP;
    private Double ktjgCP;
    private Double rjzsCP;
    private Double hbyqCCP;
    private Double nsdk100CP;

    public List<AgClosePriceBO> toBO() {
        List<AgClosePriceBO> list = new ArrayList<>();
        list.add(AgClosePriceBO.builder().time(time).type("sz50").closePrice(sz50CP).build());
        list.add(AgClosePriceBO.builder().time(time).type("szzs").closePrice(szzsCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("hs300").closePrice(hs300CP).build());
        list.add(AgClosePriceBO.builder().time(time).type("szcz").closePrice(szczCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("kc50").closePrice(kc50CP).build());
        list.add(AgClosePriceBO.builder().time(time).type("zz1000").closePrice(zz1000CP).build());
        list.add(AgClosePriceBO.builder().time(time).type("zz2000").closePrice(zz2000CP).build());
        list.add(AgClosePriceBO.builder().time(time).type("bz50").closePrice(bz50CP).build());
        list.add(AgClosePriceBO.builder().time(time).type("hskjzs").closePrice(hskjzsCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("zq").closePrice(zqCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("ysjs").closePrice(ysjsCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("gfcy").closePrice(gfcyCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("ktjg").closePrice(ktjgCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("rjzs").closePrice(rjzsCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("hbyqC").closePrice(hbyqCCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("nsdk100").closePrice(nsdk100CP).build());

        return list;
    }
}
