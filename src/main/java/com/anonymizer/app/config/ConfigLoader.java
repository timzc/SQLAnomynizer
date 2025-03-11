package com.anonymizer.app.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and provides access to application configuration
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "config.properties";
    
    private Configuration config;
    
    public ConfigLoader() {
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            Configurations configs = new Configurations();
            config = configs.properties(new File("src/main/resources/" + CONFIG_FILE));
            logger.info("Configuration loaded successfully");
        } catch (ConfigurationException e) {
            logger.error("Error loading configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    public String getDatabaseUrl() {
        return config.getString("db.url") + config.getString("db.name");
    }
    
    public String getDatabaseUser() {
        return config.getString("db.user");
    }
    
    public String getDatabasePassword() {
        return config.getString("db.password");
    }
    
    /**
     * 获取需要处理的所有表名
     */
    public List<String> getTables() {
        String tablesStr = config.getString("tables", "");
        if (tablesStr.isEmpty()) {
            logger.warn("No tables specified in configuration");
            return List.of();
        }
        return Arrays.asList(tablesStr.split(","));
    }
    
    /**
     * 获取指定表需要脱敏的列
     * 
     * @param tableName 表名
     * @return 需要脱敏的列名列表
     */
    public List<String> getColumnsToAnonymize(String tableName) {
        String columnsStr = config.getString(tableName + ".columns", "");
        if (columnsStr.isEmpty()) {
            logger.warn("No columns specified for table: {}", tableName);
            return List.of();
        }
        return Arrays.asList(columnsStr.split(","));
    }
    
    /**
     * 获取指定表的列类型映射
     * 
     * @param tableName 表名
     * @return 列名到类型的映射
     */
    public Map<String, String> getColumnTypes(String tableName) {
        Map<String, String> columnTypes = new HashMap<>();
        for (String column : getColumnsToAnonymize(tableName)) {
            String type = config.getString("column.type." + tableName + "." + column);
            if (type != null) {
                columnTypes.put(column, type);
            } else {
                logger.warn("No type specified for column: {} in table: {}", column, tableName);
            }
        }
        return columnTypes;
    }
    
    /**
     * 获取所有配置的表和列信息
     * 
     * @return 表名到列名集合的映射
     */
    public Map<String, Set<String>> getAllTablesAndColumns() {
        Map<String, Set<String>> result = new HashMap<>();
        
        for (String tableName : getTables()) {
            Set<String> columns = new HashSet<>(getColumnsToAnonymize(tableName));
            if (!columns.isEmpty()) {
                result.put(tableName, columns);
            }
        }
        
        return result;
    }
} 