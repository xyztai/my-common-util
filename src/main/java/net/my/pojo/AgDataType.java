package net.my.pojo;

public enum AgDataType {
    Stock_Code("t_stock_code"),
    ETF("t_etf"),
    Index("t_index");

    private final String tableName;

    public String getTableName() { return tableName; }

    AgDataType(String tableName) {
        this.tableName = tableName;
    }

}
