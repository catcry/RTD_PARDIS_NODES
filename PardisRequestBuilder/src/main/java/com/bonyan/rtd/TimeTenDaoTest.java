package com.bonyan.rtd;

import com.bonyan.rtd.dao.TrsHandler;

public class TimeTenDaoTest {

    public static void main(String[] args) {
        // Set the DSN, username, and password dynamically
        String dsn = "ccFdcDS";
        String username = "YourUsername";
        String password = "YourPassword";

        // Instantiate the DAO
        TrsHandler dao = new TrsHandler(dsn, username, password);

        // Test the connection and methods
        try {
            // Create a table
            dao.createTable("test_table", "id INT PRIMARY KEY, name VARCHAR(50)");
            System.out.println("Table created successfully.");

            // Insert data
            dao.insertIntoTable("test_table", "id, name", "1, 'John Doe'");
            System.out.println("Data inserted successfully.");

            // Check if records exist
            boolean hasRecords = dao.hasRecords("test_table");
            System.out.println("Table has records: " + hasRecords);

            // Select data
            dao.getRecord("test_table", "*");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the connection
            dao.closeConn();
        }
    }
}
