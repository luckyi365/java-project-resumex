package com.resumex;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Centralized configuration loader for ResumeX.
 * <p>
 * Resolution order for every key:
 * <ol>
 *   <li>Environment variable (name specified per call)</li>
 *   <li>{@code config.properties} file in the working directory</li>
 *   <li>{@code config.properties} on the classpath</li>
 *   <li>Hard-coded default supplied by the caller</li>
 * </ol>
 * No secrets are ever hard-coded in source.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());
    private static final Properties PROPS = new Properties();

    static {
        // 1. Try working-directory file first (next to the JAR)
        File local = new File("config.properties");
        if (local.exists()) {
            try (InputStream is = new FileInputStream(local)) {
                PROPS.load(is);
                LOG.info("Loaded config from " + local.getAbsolutePath());
            } catch (IOException e) {
                LOG.warning("Failed to read local config.properties: " + e.getMessage());
            }
        } else {
            // 2. Fall back to classpath resource
            try (InputStream is = AppConfig.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (is != null) {
                    PROPS.load(is);
                    LOG.info("Loaded config from classpath.");
                }
            } catch (IOException e) {
                LOG.warning("Failed to read classpath config.properties: " + e.getMessage());
            }
        }
    }

    private AppConfig() { /* utility class */ }

    /**
     * Resolves a configuration value.
     *
     * @param envVar       the environment-variable name to check first
     * @param propKey      the {@code config.properties} key to check second
     * @param defaultValue fallback if neither source provides a value
     * @return resolved value, or {@code defaultValue}
     */
    public static String get(String envVar, String propKey, String defaultValue) {
        String v = System.getenv(envVar);
        if (v != null && !v.isBlank()) return v;
        v = PROPS.getProperty(propKey);
        if (v != null && !v.isBlank()) return v;
        return defaultValue;
    }

    /**
     * Resolves a required configuration value.
     *
     * @param envVar  the environment-variable name
     * @param propKey the property-file key
     * @return the resolved value
     * @throws IllegalStateException if the value is missing from all sources
     */
    public static String require(String envVar, String propKey) {
        String v = get(envVar, propKey, null);
        if (v == null) {
            throw new IllegalStateException(
                    "Missing required config: set env var " + envVar
                    + " or property " + propKey + " in config.properties");
        }
        return v;
    }
}
