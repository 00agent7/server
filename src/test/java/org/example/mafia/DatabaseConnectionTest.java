package org.example.mafia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseConnection() throws SQLException {
        assertNotNull(dataSource, "DataSource should not be null");
        
        Connection connection = dataSource.getConnection();
        assertNotNull(connection, "Connection should not be null");
        
        // Print connection info for debugging
        System.out.println("[DEBUG_LOG] Connected to database: " + connection.getMetaData().getDatabaseProductName());
        System.out.println("[DEBUG_LOG] Database version: " + connection.getMetaData().getDatabaseProductVersion());
        
        // Test that we can execute a simple query
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertNotNull(result, "Query result should not be null");
        assertTrue(result == 1, "Query result should be 1");
        
        System.out.println("[DEBUG_LOG] Database connection test successful");
        
        connection.close();
    }
}