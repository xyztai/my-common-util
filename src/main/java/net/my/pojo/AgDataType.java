package net.my.pojo;

public enum AgDataType {
    STOCK_CODE("t_stock_code", "t_eastmoney_node"),
    ETF("t_etf", "t_eastmoney_node_etf"),
    INDEX("t_index", "t_eastmoney_node_index");

    private final String codeTableName;
    private final String nodeTableName;

    public String getCodeTableName() { return codeTableName; }
    public String getNodeTableName() { return nodeTableName; }

    AgDataType(String codeTableName, String nodeTableName) {
        this.codeTableName = codeTableName;
        this.nodeTableName = nodeTableName;
    }

}
