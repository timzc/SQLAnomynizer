package com.anonymizer.app;

import com.anonymizer.app.config.ConfigLoader;
import com.anonymizer.app.db.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * 数据脱敏应用程序主类
 */
public class DataAnonymizer {
    private static final Logger logger = LoggerFactory.getLogger(DataAnonymizer.class);
    
    public static void main(String[] args) {
        logger.info("Starting Data Anonymizer");
        
        try {
            // 加载配置
            ConfigLoader configLoader = new ConfigLoader();
            
            // 初始化数据库服务
            DatabaseService dbService = new DatabaseService(configLoader, configLoader.getProperties());
            
            // 连接数据库
            dbService.connect();
            
            // 对所有配置的表进行脱敏
            dbService.anonymizeAllTables();
            
            // 断开数据库连接
            dbService.disconnect();
            
            logger.info("Data anonymization completed successfully");
        } catch (SQLException e) {
            logger.error("Database error: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
    }
} 