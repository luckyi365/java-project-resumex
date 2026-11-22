package com.resumex.config;

import com.resumex.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modern Connection Manager using HikariCP connection pooling and Singleton pattern.
 */
public final class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());
    private static volatile HikariDataSource dataSource;

    private DatabaseConnection() {}

    private static synchronized void initializeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        String url = AppConfig.get("RESUMEX_DB_URL", "db.url",
                "jdbc:mysql://localhost:3306/resumex_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = AppConfig.get("RESUMEX_DB_USER", "db.user", "root");
        String password = AppConfig.get("RESUMEX_DB_PASSWORD", "db.password", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool tuning for responsive UI performance
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(600000);
        config.setPoolName("ResumeX-HikariPool");

        try {
            dataSource = new HikariDataSource(config);
            LOG.info("HikariCP connection pool initialized successfully for ResumeX.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to initialize HikariCP connection pool: " + e.getMessage(), e);
            throw new RuntimeException("Database pool initialization failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }
        return dataSource.getConnection();
    }

    public static boolean isAvailable() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOG.info("HikariCP connection pool shut down.");
        }
    }
}
