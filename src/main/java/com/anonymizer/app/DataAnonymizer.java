package com.anonymizer.app;

import com.anonymizer.app.config.ConfigLoader;
import com.anonymizer.app.db.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Main class for the Data Anonymizer application
 */
public class DataAnonymizer {
    private static final Logger logger = LoggerFactory.getLogger(DataAnonymizer.class);
    
    public static void main(String[] args) {
        logger.info("Starting Data Anonymizer");
        
        try {
            // Load configuration
            ConfigLoader configLoader = new ConfigLoader();
            
            // Initialize database service
            DatabaseService dbService = new DatabaseService(configLoader);
            
            // Connect to the database
            dbService.connect();
            
            // Anonymize the data in all configured tables
            dbService.anonymizeAllTables();
            
            // Disconnect from the database
            dbService.disconnect();
            
            logger.info("Data anonymization completed successfully");
        } catch (SQLException e) {
            logger.error("Database error: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
    }
} 