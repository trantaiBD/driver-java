/*
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 *
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

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

public class ByteHouseSettingsITest extends AbstractITest {
    @Test
    public void successfullySetAnsiSQL() throws Exception {
        withStatement(statement -> {
            String databaseName = getDatabaseName();
            String tableName = databaseName + "." + getTableName();

            try {
                statement.execute(String.format("CREATE DATABASE %s", databaseName));
                statement.execute(String.format("CREATE TABLE %s(id Int)"
                        + " ENGINE=CnchMergeTree() order by tuple()", tableName));
            }
            finally {
                statement.execute(String.format("DROP DATABASE %s", databaseName));
            }
        }, "ansi_sql", true);
    }

    @Ignore
    public void successfullySetDictTableFullMode() throws Exception {
        withStatement(statement -> {
            String databaseName = getDatabaseName();
            String tableName = databaseName + "." + getTableName();

            try {
                statement.execute(String.format("CREATE DATABASE IF NOT EXISTS %s", databaseName));
                statement.execute(String.format("CREATE TABLE IF NOT EXISTS %s(id Int)"
                        + " ENGINE=CnchMergeTree() order by tuple()", tableName));
            }
            finally {
                statement.execute(String.format("DROP DATABASE %s", databaseName));
            }
        }, "dict_table_full_mode", 1);
    }
}
