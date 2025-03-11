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
import java.util.Set;

/**
 * Service for database operations
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private final ConfigLoader configLoader;
    private Connection connection;
    
    public DatabaseService(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }
    
    /**
     * Connect to the database
     */
    public void connect() throws SQLException {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Connect to the database
            connection = DriverManager.getConnection(
                    configLoader.getDatabaseUrl(),
                    configLoader.getDatabaseUser(),
                    configLoader.getDatabasePassword()
            );
            
            logger.info("Connected to database: {}", configLoader.getDatabaseUrl());
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC driver not found", e);
            throw new SQLException("MySQL JDBC driver not found", e);
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
        Map<String, Set<String>> tablesAndColumns = configLoader.getAllTablesAndColumns();
        
        if (tablesAndColumns.isEmpty()) {
            logger.warn("No tables configured for anonymization");
            return;
        }
        
        int totalTablesProcessed = 0;
        int totalRowsProcessed = 0;
        
        for (Map.Entry<String, Set<String>> entry : tablesAndColumns.entrySet()) {
            String tableName = entry.getKey();
            Set<String> columns = entry.getValue();
            
            int rowsProcessed = anonymizeTable(tableName, new ArrayList<>(columns));
            if (rowsProcessed > 0) {
                totalTablesProcessed++;
                totalRowsProcessed += rowsProcessed;
            }
        }
        
        logger.info("Anonymization completed for {} tables, {} rows processed", 
                totalTablesProcessed, totalRowsProcessed);
    }
    
    /**
     * 获取指定表的所有列名
     * 
     * @param tableName 表名
     * @return 列名列表
     */
    public List<String> getTableColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        
        String query = "SELECT * FROM " + tableName + " LIMIT 1";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
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
    private int anonymizeTable(String tableName, List<String> columnsToAnonymize) throws SQLException {
        logger.info("Processing table: {}", tableName);
        
        // 获取表的列类型配置
        Map<String, String> columnTypes = configLoader.getColumnTypes(tableName);
        
        // 获取表的所有列
        List<String> allColumns;
        try {
            allColumns = getTableColumns(tableName);
        } catch (SQLException e) {
            logger.error("Error getting columns for table {}: {}", tableName, e.getMessage());
            return 0;
        }
        
        if (allColumns.isEmpty()) {
            logger.warn("Table {} appears to be empty or does not exist", tableName);
            return 0;
        }
        
        // 过滤出表中实际存在的列
        List<String> validColumnsToAnonymize = new ArrayList<>();
        for (String column : columnsToAnonymize) {
            if (allColumns.contains(column)) {
                validColumnsToAnonymize.add(column);
            } else {
                logger.warn("Column {} not found in table {}", column, tableName);
            }
        }
        
        if (validColumnsToAnonymize.isEmpty()) {
            logger.warn("No valid columns to anonymize in table {}", tableName);
            return 0;
        }
        
        int totalRowsProcessed = 0;
        
        // 获取表的主键列
        String primaryKeyColumn = getPrimaryKeyColumn(tableName);
        if (primaryKeyColumn == null || primaryKeyColumn.isEmpty()) {
            logger.warn("No primary key found for table {}, using first column as identifier", tableName);
            primaryKeyColumn = allColumns.get(0);
        }
        
        // 获取表中的所有行
        String selectQuery = "SELECT * FROM " + tableName;
        try (Statement selectStmt = connection.createStatement();
             ResultSet rs = selectStmt.executeQuery(selectQuery)) {
            
            // 对每一行进行处理
            while (rs.next()) {
                String idValue = rs.getString(primaryKeyColumn);
                
                // 构建更新查询
                StringBuilder updateQuery = new StringBuilder();
                updateQuery.append("UPDATE ").append(tableName).append(" SET ");
                
                // 准备SET部分
                boolean firstColumn = true;
                for (String column : validColumnsToAnonymize) {
                    if (rs.getString(column) == null) {
                        continue;
                    }
                    
                    if (!firstColumn) {
                        updateQuery.append(", ");
                    }
                    updateQuery.append(column).append(" = ?");
                    firstColumn = false;
                }
                
                // 如果没有有效列需要更新，则跳过
                if (firstColumn) {
                    continue;
                }
                
                // 添加WHERE子句
                updateQuery.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
                
                // 执行更新
                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery.toString())) {
                    int paramIndex = 1;
                    
                    // 设置每列的值
                    for (String column : validColumnsToAnonymize) {
                        String originalValue = rs.getString(column);
                        if (originalValue == null) {
                            continue;
                        }
                        
                        // 获取该列的脱敏规则
                        String columnType = columnTypes.get(column);
                        AnonymizationRule rule = AnonymizationRules.getRule(columnType);
                        
                        // 应用规则
                        String anonymizedValue = rule.anonymize(originalValue);
                        
                        // 设置参数
                        updateStmt.setString(paramIndex++, anonymizedValue);
                        
                        // 记录脱敏过程
                        logger.debug("Anonymizing {}.{} ({}): {} -> {}", 
                                tableName, column, columnType, originalValue, anonymizedValue);
                    }
                    
                    // 设置ID参数
                    updateStmt.setString(paramIndex, idValue);
                    
                    // 执行更新
                    int rowsUpdated = updateStmt.executeUpdate();
                    totalRowsProcessed += rowsUpdated;
                    logger.debug("Updated {} rows for ID {} in table {}", rowsUpdated, idValue, tableName);
                }
            }
        }
        
        logger.info("Anonymization completed for table {}: {} rows processed", tableName, totalRowsProcessed);
        return totalRowsProcessed;
    }
    
    /**
     * 获取表的主键列名
     * 
     * @param tableName 表名
     * @return 主键列名，如果没有找到则返回null
     */
    private String getPrimaryKeyColumn(String tableName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
                if (rs.next()) {
                    return rs.getString("COLUMN_NAME");
                }
            }
        } catch (SQLException e) {
            logger.warn("Error getting primary key for table {}: {}", tableName, e.getMessage());
        }
        return null;
    }
} 