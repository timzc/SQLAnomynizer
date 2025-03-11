package com.anonymizer.app.db;

/**
 * 数据库方言接口，用于处理不同数据库的SQL差异
 */
public interface DatabaseDialect {
    
    /**
     * 获取查询表结构的SQL
     * 
     * @param tableName 表名
     * @return SQL语句
     */
    String getTableStructureQuery(String tableName);
    
    /**
     * 获取查询表所有数据的SQL
     * 
     * @param tableName 表名
     * @return SQL语句
     */
    String getSelectAllQuery(String tableName);
    
    /**
     * 获取更新语句
     * 
     * @param tableName 表名
     * @param columns 需要更新的列
     * @param primaryKeyColumn 主键列名
     * @return SQL语句
     */
    String getUpdateQuery(String tableName, String[] columns, String primaryKeyColumn);
    
    /**
     * 获取主键查询语句
     * 
     * @param tableName 表名
     * @return SQL语句
     */
    String getPrimaryKeyQuery(String tableName);
    
    /**
     * 创建方言实例
     * 
     * @param dbType 数据库类型
     * @return 数据库方言实例
     */
    static DatabaseDialect create(DatabaseType dbType) {
        switch (dbType) {
            case ORACLE:
                return new OracleDialect();
            case POSTGRESQL:
                return new PostgreSQLDialect();
            case MYSQL:
            default:
                return new MySQLDialect();
        }
    }
} 