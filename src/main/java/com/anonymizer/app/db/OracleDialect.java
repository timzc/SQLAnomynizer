package com.anonymizer.app.db;

/**
 * Oracle数据库方言实现
 */
public class OracleDialect implements DatabaseDialect {
    
    @Override
    public String getTableStructureQuery(String tableName) {
        return "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
    }
    
    @Override
    public String getSelectAllQuery(String tableName) {
        return "SELECT * FROM " + tableName;
    }
    
    @Override
    public String getUpdateQuery(String tableName, String[] columns, String primaryKeyColumn) {
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ").append(tableName).append(" SET ");
        
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                query.append(", ");
            }
            query.append(columns[i]).append(" = ?");
        }
        
        query.append(" WHERE ").append(primaryKeyColumn).append(" = ?");
        return query.toString();
    }
    
    @Override
    public String getPrimaryKeyQuery(String tableName) {
        return "SELECT cols.column_name FROM all_constraints cons, all_cons_columns cols " +
               "WHERE cols.table_name = '" + tableName.toUpperCase() + "' " +
               "AND cons.constraint_type = 'P' " +
               "AND cons.constraint_name = cols.constraint_name " +
               "AND cons.owner = cols.owner";
    }
} 