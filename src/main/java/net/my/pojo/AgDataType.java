package net.my.pojo;

public enum AgDataType {
    STOCK_CODE("t_stock", "t_stock_raw"),
    ETF("t_etf", "t_etf_raw"),
    INDEX("t_index", "t_index_raw");

    private final String codeTableName;
    private final String nodeTableName;

    public String getCodeTableName() { return codeTableName; }
    public String getNodeTableName() { return nodeTableName; }

    AgDataType(String codeTableName, String nodeTableName) {
        this.codeTableName = codeTableName;
        this.nodeTableName = nodeTableName;
    }

}
