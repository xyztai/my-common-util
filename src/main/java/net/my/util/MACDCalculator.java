package net.my.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MACDCalculator {

    public static class MACDResult {
        public double ema12;
        public double ema26;
        public double dif;
        public double dea;
        public double macd;

        public MACDResult(double ema12, double ema26, double dif, double dea, double macd) {
            this.ema12 = ema12;
            this.ema26 = ema26;
            this.dif = dif;
            this.dea = dea;
            this.macd = macd;
        }
    }

    public static List<MACDResult> calculate(List<Double> prices) {
        List<MACDResult> results = new ArrayList<>();
        if (prices == null || prices.isEmpty()) {
            return results;
        }

        double ema12 = 0;
        double ema26 = 0;
        double dea = 0;

        for (int i = 0; i < prices.size(); i++) {
            double price = prices.get(i);

            if (i == 0) {
                ema12 = price;
                ema26 = price;
                dea = 0;
            } else {
                ema12 = (2.0 * price + 11.0 * ema12) / 13.0; // 2 / (12 + 1)
                ema26 = (2.0 * price + 25.0 * ema26) / 27.0; // 2 / (26 + 1)
            }

            double dif = ema12 - ema26;

            if (i == 0) {
                dea = dif;
            } else {
                dea = (2.0 * dif + 8.0 * dea) / 10.0; // 2 / (9 + 1)
            }

            double macd = (dif - dea) * 2.0;

            results.add(new MACDResult(ema12, ema26, dif, dea, macd));
        }

        return results;
    }

    public static void main(String[] args) {
        List<Double> mockPrices = Arrays.asList(
                10.0, 10.5, 10.2, 10.8, 11.0, 10.7, 11.2, 11.5, 11.3, 12.0,
                11.8, 12.2, 12.5, 12.1, 12.8
        );

        List<MACDResult> macdResults = calculate(mockPrices);
        for (int i = 0; i < macdResults.size(); i++) {
            MACDResult r = macdResults.get(i);
            System.out.printf("Day %d: DIF=%.4f, DEA=%.4f, MACD=%.4f\n", i + 1, r.dif, r.dea, r.macd);
        }
    }
}
