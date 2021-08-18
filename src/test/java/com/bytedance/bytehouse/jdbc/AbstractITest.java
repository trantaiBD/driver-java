/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytedance.bytehouse.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;

public class AbstractITest implements Serializable {
    private static final String HOST = "HOST";
    private static final String PORT = "PORT";
    private static final String USER = "USER";

    private Properties TestConfigs;

    protected String getHost() {
        return TestConfigs.getProperty(HOST);
    }

    protected String getPort() {
        return TestConfigs.getProperty(PORT);
    }

    protected String getUrl() {
        return String.format("jdbc:bytehouse://%s:%s", getHost(), getPort());
    }

    protected String getUsername() {
        return TestConfigs.getProperty(USER);
    }

    protected Connection getConnection(Object... params) throws SQLException {
        loadTestConfigs(params);
        final DataSource dataSource = new BalancedByteHouseDataSource(getUrl(), TestConfigs);
        return dataSource.getConnection();
    }

    protected BalancedByteHouseDataSource getDataSource(String url, Object... params) throws SQLException {
        loadTestConfigs(params);
        final DataSource dataSource = new BalancedByteHouseDataSource(url, TestConfigs);
        return (BalancedByteHouseDataSource) dataSource;
    }

    private void loadTestConfigs(Object... params) {
        Properties envProperty = new Properties();
        try (InputStream input = Files.newInputStream(Paths
                .get("src/test/resources/env.properties"))) {
            envProperty.load(input);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        String envName = envProperty.getProperty("env");
        try (InputStream input = Files.newInputStream(Paths
                .get("src/test/resources/" + envName + "-config.properties"))) {
            TestConfigs = new Properties();
            TestConfigs.load(input);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        for (int i = 0; i + 1 < params.length; i = i + 2) {
            TestConfigs.setProperty(params[i].toString(), params[i+1].toString());
        }
    }

    protected void withNewConnection(WithConnection withConnection, Object... args) throws Exception {
        try (Connection connection = getConnection(args)) {
            withConnection.apply(connection);
        }
    }

    protected void withNewConnection(DataSource ds, WithConnection withConnection) throws Exception {
        try (Connection connection = ds.getConnection()) {
            withConnection.apply(connection);
        }
    }

    protected void withStatement(Connection connection, WithStatement withStatement) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            withStatement.apply(stmt);
        }
    }

    protected void withStatement(WithStatement withStatement, Object... args) throws Exception {
        withNewConnection(connection -> withStatement(connection, withStatement), args);
    }

    protected void withPreparedStatement(Connection connection,
                                         String sql,
                                         WithPreparedStatement withPreparedStatement) throws Exception {
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            withPreparedStatement.apply(pstmt);
        }
    }

    protected void withPreparedStatement(String sql,
                                         WithPreparedStatement withPreparedStatement,
                                         Object... args) throws Exception {
        withNewConnection(connection -> withPreparedStatement(connection, sql, withPreparedStatement), args);
    }

    @FunctionalInterface
    public interface WithConnection {
        void apply(Connection connection) throws Exception;
    }

    @FunctionalInterface
    public interface WithStatement {
        void apply(Statement stmt) throws Exception;
    }

    @FunctionalInterface
    public interface WithPreparedStatement {
        void apply(PreparedStatement pstmt) throws Exception;
    }
}
