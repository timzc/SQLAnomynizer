package com.anonymizer.app.db;

import com.anonymizer.app.anonymizer.AnonymizationRule;
import com.anonymizer.app.anonymizer.AnonymizationRules;
import com.anonymizer.app.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Service for database operations
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private final ConfigLoader configLoader;
    private Connection connection;
    private Properties config;
    private DatabaseDialect dialect;
    
    public DatabaseService(ConfigLoader configLoader, Properties config) {
        this.configLoader = configLoader;
        this.config = config;
        String dbType = config.getProperty("database.type", "mysql");
        this.dialect = DatabaseDialect.create(DatabaseType.fromString(dbType));
    }
    
    /**
     * Connect to the database
     */
    public void connect() throws SQLException {
        String url = config.getProperty("database.url");
        String username = config.getProperty("database.username");
        String password = config.getProperty("database.password");
        
        try {
            Class.forName(DatabaseType.fromString(config.getProperty("database.type", "mysql")).getDriverClassName());
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found", e);
        }
    }
    
    /**
     * Close the database connection
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Disconnected from database");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }
    
    /**
     * 脱敏所有配置的表
     */
    public void anonymizeAllTables() throws SQLException {
        List<String> tables = configLoader.getTables();
        int totalTables = tables.size();
        int processedTables = 0;
        
        logger.info("Starting anonymization of {} tables", totalTables);
        
        for (String tableName : tables) {
            try {
                List<String> columnsToAnonymize = configLoader.getColumnsToAnonymize(tableName);
                anonymizeTable(tableName, columnsToAnonymize);
                processedTables++;
                logger.info("Processed table {}: {}/{}", tableName, processedTables, totalTables);
            } catch (SQLException e) {
                logger.error("Error processing table {}: {}", tableName, e.getMessage());
                throw e;
            }
        }
        
        logger.info("Completed anonymization of {} tables", processedTables);
    }
    
    /**
     * 获取指定表的所有列名
     * 
     * @param tableName 表名
     * @return 列名列表
     */
    public List<String> getTableColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(dialect.getTableStructureQuery(tableName))) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columns.add(metaData.getColumnName(i));
            }
        }
        return columns;
    }
    
    /**
     * 脱敏指定表中的数据
     * 
     * @param tableName 表名
     * @param columnsToAnonymize 需要脱敏的列
     * @return 处理的行数
     */
    private void anonymizeTable(String tableName, List<String> columnsToAnonymize) throws SQLException {
        String primaryKeyColumn = getPrimaryKeyColumn(tableName);
        String[] columns = columnsToAnonymize.toArray(new String[0]);
        String updateQuery = dialect.getUpdateQuery(tableName, columns, primaryKeyColumn);
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(dialect.getSelectAllQuery(tableName));
             PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
            
            int batchSize = 0;
            while (rs.next()) {
                int paramIndex = 1;
                for (String column : columnsToAnonymize) {
                    String originalValue = rs.getString(column);
                    String anonymizedValue = anonymizeValue(originalValue, getAnonymizationRule(tableName, column));
                    pstmt.setString(paramIndex++, anonymizedValue);
                }
                pstmt.setObject(paramIndex, rs.getObject(primaryKeyColumn));
                pstmt.addBatch();
                
                if (++batchSize % 1000 == 0) {
                    pstmt.executeBatch();
                    connection.commit();
                }
            }
            
            if (batchSize % 1000 != 0) {
                pstmt.executeBatch();
                connection.commit();
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    /**
     * 获取表的主键列名
     * 
     * @param tableName 表名
     * @return 主键列名，如果没有找到则返回null
     */
    private String getPrimaryKeyColumn(String tableName) throws SQLException {
        String primaryKeyQuery = dialect.getPrimaryKeyQuery(tableName);
        if (primaryKeyQuery != null) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(primaryKeyQuery)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } else {
            // 使用JDBC元数据API获取主键
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
                if (rs.next()) {
                    return rs.getString("COLUMN_NAME");
                }
            }
        }
        throw new SQLException("No primary key found for table: " + tableName);
    }
    
    /**
     * 获取指定表列的脱敏规则
     * 
     * @param tableName 表名
     * @param columnName 列名
     * @return 脱敏规则
     */
    private AnonymizationRule getAnonymizationRule(String tableName, String columnName) {
        String columnType = configLoader.getColumnTypes(tableName).get(columnName);
        return AnonymizationRules.getRule(columnType);
    }
    
    /**
     * 使用脱敏规则处理值
     * 
     * @param originalValue 原始值
     * @param rule 脱敏规则
     * @return 脱敏后的值
     */
    private String anonymizeValue(String originalValue, AnonymizationRule rule) {
        if (originalValue == null) {
            return null;
        }
        return rule.anonymize(originalValue);
    }
} 