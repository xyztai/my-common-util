package net.my.pojo;

public enum AgDataType {
    STOCK_CODE("t_stock_code"),
    ETF("t_etf"),
    INDEX("t_index");

    private final String tableName;

    public String getTableName() { return tableName; }

    AgDataType(String tableName) {
        this.tableName = tableName;
    }

}
