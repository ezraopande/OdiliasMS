package com.odilias.odms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    // Local XAMPP MySQL connection
    private static final String URL = "jdbc:mysql://localhost:3306/hospital" +
            "?cachePrepStmts=true" +
            "&prepStmtCacheSize=250" +
            "&prepStmtCacheSqlLimit=2048" +
            "&useServerPrepStmts=true" +
            "&useLocalSessionState=true" +
            "&rewriteBatchedStatements=true" +
            "&cacheResultSetMetadata=true" +
            "&cacheServerConfiguration=true" +
            "&elideSetAutoCommits=true" +
            "&maintainTimeStats=false" +
            "&connectTimeout=5000" +
            "&socketTimeout=30000";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // HikariCP connection pooling optimizations to support multiple users
            // seamlessly
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(2);
            config.setIdleTimeout(60000); // 1 minute idle
            config.setConnectionTimeout(15000); // 15 sec timeout
            config.setMaxLifetime(1800000); // 30 minutes max lifetime
            config.setLeakDetectionThreshold(10000); // 10 secs leak threshold
            config.setValidationTimeout(5000); // 5 secs validation

            dataSource = new HikariDataSource(config);
            System.out.println("HikariCP Connection Pool Initialized!");
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE: HikariCP connection pool could not be initialized.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        // Obtains a pre-warmed physical connection quickly from the pool
        return dataSource.getConnection();
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                // Return the connection to the pool rather than actively killing the TCP socket
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
