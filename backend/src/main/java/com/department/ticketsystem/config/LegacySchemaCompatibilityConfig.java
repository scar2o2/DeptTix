package com.department.ticketsystem.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LegacySchemaCompatibilityConfig implements CommandLineRunner {

    private static final String LEGACY_PASSWORD_PLACEHOLDER = "FIREBASE_AUTH_LEGACY_PLACEHOLDER";

    private final DataSource dataSource;

    public LegacySchemaCompatibilityConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasPasswordColumn(connection)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "UPDATE users SET password = '" + LEGACY_PASSWORD_PLACEHOLDER + "' "
                                + "WHERE password IS NULL OR password = ''");
                statement.executeUpdate(
                        "ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NOT NULL "
                                + "DEFAULT '" + LEGACY_PASSWORD_PLACEHOLDER + "'");
            }
        }
    }

    private boolean hasPasswordColumn(Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, "users", "password")) {
            return columns.next();
        }
    }
}
