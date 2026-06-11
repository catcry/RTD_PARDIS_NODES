package com.bonyan.rtd.dao;


import java.sql.*;

public class TrsHandler {

    private Connection connection;
    public TrsHandler (String dsn, String username, String password) {
        try {
            // Load the TimesTen driver
            Class.forName("com.timesten.jdbc.TimesTenDriver");

            String dbUrl = "jdbc:timesten:direct:dsn=" + dsn;

            connection = DriverManager.getConnection(dbUrl, username, password);

        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to establish connection to TimesTen", e);
        }
    }

    // -------------------------------     Create Table    -----------------------------------
    public void createTable(String tableName, String tableDefinition) {
        String query = String.format("CREATE TABLE %s (%s)", tableName, tableDefinition);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            // Handle exception without printing
            throw new RuntimeException("Error creating table " + tableName, e);
        }
    }
    // ------------------------------  Insert Into     ------------------------------------------
    public void insertIntoTable(String tableName, String columns, String values) {
        String query = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            // Handle exception without printing
            throw new RuntimeException("Error inserting data into table " + tableName, e);
        }
    }


    //  ------------------------------     Has Any Records    -------------------------------------

    public boolean hasRecords(String tableName) {
        String query = String.format("SELECT 1 FROM %s ", tableName);
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            return rs.next();
        } catch (SQLException e) {
            // Handle exception without printing
            throw new RuntimeException("Error checking records in table " + tableName, e);
        }
    }

    // -------------------------------      Select From Table    -----------------------------------
    public ResultSet getRecord(String tableName, String columns) {
        String query = String.format("SELECT %s FROM %s", columns, tableName);
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            // Handle exception without printing
            throw new RuntimeException("Error selecting data from table " + tableName, e);
        }
    }


    // -------------------------------     Close Connection    -----------------------------------
    public void closeConn() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Handle the exception as needed, no print statements
                throw new RuntimeException("Failed to close the connection", e);
            }
        }
    }


}
