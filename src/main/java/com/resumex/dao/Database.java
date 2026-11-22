package com.resumex.dao;

import com.resumex.AppConfig;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC connection helper and automatic schema bootstrapper.
 * <p>
 * On first call to {@link #getConnection()}, the helper creates the
 * {@code resumex} database and all required tables if they do not exist.
 * Connection parameters come from environment variables or
 * {@code config.properties} (see {@link AppConfig}).
 */
public final class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    /** Tracks whether the schema bootstrap has already run in this JVM. */
    private static volatile boolean schemaReady = false;

    /** Flag indicating whether the database is reachable. */
    private static volatile boolean available = false;

    private Database() { /* utility class */ }

    // ---- Config resolution ----

    private static String serverUrl() {
        // URL without a specific database — used for CREATE DATABASE
        String url = AppConfig.get("RESUMEX_DB_URL", "db.url",
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        int authorityStart = url.indexOf("://");
        if (authorityStart < 0) return url;

        int queryStart = url.indexOf('?', authorityStart + 3);
        int pathStart = url.indexOf('/', authorityStart + 3);
        if (pathStart < 0 || (queryStart >= 0 && pathStart > queryStart)) return url;

        int pathEnd = queryStart >= 0 ? queryStart : url.length();
        return url.substring(0, pathStart + 1) + url.substring(pathEnd);
    }

    private static String dbUrl() {
        String url = serverUrl();
        int queryStart = url.indexOf('?');
        String base = queryStart >= 0 ? url.substring(0, queryStart) : url;
        String options = queryStart >= 0 ? url.substring(queryStart) : "";
        if (!base.endsWith("/")) base += "/";
        return base + "resumex" + options;
    }

    private static String user() {
        return AppConfig.get("RESUMEX_DB_USER", "db.user", "root");
    }

    private static String password() {
        return AppConfig.get("RESUMEX_DB_PASSWORD", "db.password", "");
    }

    // ---- Public API ----

    /**
     * Returns a fresh JDBC connection to the {@code resumex} database.
     * The caller is responsible for closing it (use try-with-resources).
     *
     * @return a live {@link Connection}
     * @throws SQLException if the connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (!schemaReady) {
            synchronized (Database.class) {
                if (!schemaReady) bootstrap();
            }
        }
        return DriverManager.getConnection(dbUrl(), user(), password());
    }

    /**
     * Returns whether the database was reachable during the last
     * bootstrap attempt.
     *
     * @return true if MySQL is available
     */
    public static boolean isAvailable() {
        if (!schemaReady) {
            try {
                synchronized (Database.class) {
                    if (!schemaReady) bootstrap();
                }
            } catch (Exception e) {
                // already logged inside bootstrap()
            }
        }
        return available;
    }

    // ---- Bootstrap ----

    private static void bootstrap() {
        try {
            // Ensure driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOG.warning("MySQL JDBC driver not found. DB features disabled.");
            schemaReady = true;
            return;
        }

        try (Connection conn = DriverManager.getConnection(
                serverUrl(), user(), password())) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS resumex " +
                    "DEFAULT CHARACTER SET utf8mb4 " +
                    "DEFAULT COLLATE utf8mb4_unicode_ci");
                st.executeUpdate("USE resumex");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id         INT AUTO_INCREMENT PRIMARY KEY,
                        name       VARCHAR(255)  NOT NULL,
                        email      VARCHAR(255)  NOT NULL UNIQUE,
                        created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS resumes (
                        id             INT AUTO_INCREMENT PRIMARY KEY,
                        user_id        INT           NOT NULL,
                        file_name      VARCHAR(500)  NOT NULL,
                        extracted_text LONGTEXT      NOT NULL,
                        uploaded_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS analyses (
                        id               INT AUTO_INCREMENT PRIMARY KEY,
                        resume_id        INT           NOT NULL,
                        score            INT           NOT NULL,
                        breakdown_json   TEXT          NOT NULL,
                        missing_skills   TEXT,
                        suggestions_json TEXT,
                        raw_response     LONGTEXT,
                        created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS interview_sessions (
                        id          INT AUTO_INCREMENT PRIMARY KEY,
                        resume_id   INT           NOT NULL,
                        started_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        final_score INT           DEFAULT NULL,
                        summary     TEXT          DEFAULT NULL,
                        FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS interview_messages (
                        id           INT AUTO_INCREMENT PRIMARY KEY,
                        session_id   INT          NOT NULL,
                        role         ENUM('ai','user') NOT NULL,
                        content      TEXT         NOT NULL,
                        answer_score INT          DEFAULT NULL,
                        created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);
            }
            available = true;
            LOG.info("Database schema verified / created successfully.");
        } catch (SQLException e) {
            LOG.log(Level.WARNING,
                    "Could not connect to MySQL. DB features disabled: " + e.getMessage(), e);
            available = false;
        }
        schemaReady = true;
    }
}
