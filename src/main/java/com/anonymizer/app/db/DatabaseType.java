package com.anonymizer.app.db;

/**
 * 数据库类型枚举
 */
public enum DatabaseType {
    MYSQL,
    ORACLE,
    POSTGRESQL;
    
    /**
     * 从字符串解析数据库类型
     * 
     * @param type 数据库类型字符串
     * @return 数据库类型枚举
     */
    public static DatabaseType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return MYSQL; // 默认为MySQL
        }
        
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MYSQL; // 如果解析失败，默认为MySQL
        }
    }
    
    /**
     * 获取数据库驱动类名
     * 
     * @return 驱动类名
     */
    public String getDriverClassName() {
        switch (this) {
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case ORACLE:
                return "oracle.jdbc.OracleDriver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            default:
                return "com.mysql.cj.jdbc.Driver";
        }
    }
} 