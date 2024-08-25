package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Double ljlnCP;
    private Double ndsdCP;
    private Double ymkdCP;
    private Double tqlyCP;

    public AgClosePriceDTO setValue(String type, Double value) {
        switch (type) {
            case "sz50":
                this.setSz50CP(value);
                break;
            case "szzs":
                this.setSzzsCP(value);
                break;
            case "hs300":
                this.setHs300CP(value);
                break;
            case "szcz":
                this.setSzczCP(value);
                break;
            case "kc50":
                this.setKc50CP(value);
                break;
            case "zz1000":
                this.setZz1000CP(value);
                break;
            case "zz2000":
                this.setZz2000CP(value);
                break;
            case "bz50":
                this.setBz50CP(value);
                break;
            case "hskjzs":
                this.setHskjzsCP(value);
                break;
            case "zq":
                this.setZqCP(value);
                break;
            case "ysjs":
                this.setYsjsCP(value);
                break;
            case "gfcy":
                this.setGfcyCP(value);
                break;
            case "ktjg":
                this.setKtjgCP(value);
                break;
            case "rjzs":
                this.setRjzsCP(value);
                break;
            case "hbyqC":
                this.setHbyqCCP(value);
                break;
            case "nsdk100":
                this.setNsdk100CP(value);
                break;
            case "ljln":
                this.setLjlnCP(value);
                break;
            case "ndsd":
                this.setNdsdCP(value);
                break;
            case "ymkd":
                this.setYmkdCP(value);
                break;
            case "tqly":
                this.setTqlyCP(value);
                break;
            default:
                break;
        }
        return this;
    }

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
        list.add(AgClosePriceBO.builder().time(time).type("ljln").closePrice(ljlnCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("ndsd").closePrice(ndsdCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("ymkd").closePrice(ymkdCP).build());
        list.add(AgClosePriceBO.builder().time(time).type("tqly").closePrice(tqlyCP).build());

        return list;
    }
}
