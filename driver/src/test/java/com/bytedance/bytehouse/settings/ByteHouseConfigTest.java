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

package com.bytedance.bytehouse.settings;

import com.bytedance.bytehouse.serde.SettingType;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ByteHouseConfigTest {

    @Test
    public void testDefaultByteHouseConfig() {
        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder().build();
        assertEquals("127.0.0.1", cfg.host());
        assertEquals(9000, cfg.port());
        assertEquals("", cfg.account());
        assertEquals("default", cfg.user());
        assertEquals("default", cfg.fullUsername());
        assertEquals("", cfg.password());
        assertEquals("", cfg.database());
        assertEquals(Duration.ZERO, cfg.queryTimeout());
        assertEquals(Duration.ZERO, cfg.connectTimeout());
        assertFalse(cfg.tcpKeepAlive());
        assertTrue(cfg.tcpNoDelay());
        assertFalse(cfg.secure());
        assertFalse(cfg.skipVerification());
        assertEquals(StandardCharsets.UTF_8, cfg.charset());
        assertEquals("jdbc:bytehouse://127.0.0.1:9000/?query_timeout=0&connect_timeout=0" +
                        "&charset=UTF-8&tcp_keep_alive=false&tcp_no_delay=true&secure=false&skip_verification=false" +
                        "&enable_compression=false",
                cfg.jdbcUrl());
    }

    @Ignore
    public void testByteHouseConfig() {
        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder()
                .withJdbcUrl("jdbc:bytehouse://1.2.3.4:8123/db2")
                .charset("GBK")
                .withSetting(SettingKey.allow_distributed_ddl, true)
                .build()
                .withCredentials("user", "passWorD")
                .withAccount("123");
        assertEquals("1.2.3.4", cfg.host());
        assertEquals(8123, cfg.port());
        assertEquals("123", cfg.account());
        assertEquals("user", cfg.user());
        assertEquals("123::user", cfg.fullUsername());
        assertEquals("passWorD", cfg.password());
        assertEquals("db2", cfg.database());
        assertEquals(Duration.ZERO, cfg.queryTimeout());
        assertEquals(Duration.ZERO, cfg.connectTimeout());
        assertFalse(cfg.tcpKeepAlive());
        assertTrue(cfg.tcpNoDelay());
        assertFalse(cfg.secure());
        assertFalse(cfg.skipVerification());
        assertEquals(Charset.forName("GBK"), cfg.charset());
        assertEquals("jdbc:bytehouse://1.2.3.4:8123/db2?query_timeout=0&connect_timeout=0&charset=GBK" +
                        "&tcp_keep_alive=false&tcp_no_delay=true&secure=false" +
                        "&skip_verification=false&enable_compression=false&max_block_size=65536&allow_distributed_ddl=true",
                cfg.jdbcUrl());
    }

    @Test
    public void testUndefinedSettings() {
        Properties props = new Properties();
        props.setProperty("unknown", "unknown");
        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder()
                .withProperties(props)
                .build();
        assertTrue(cfg.settings()
                .keySet()
                .stream()
                .noneMatch(settingKey -> settingKey.name().equalsIgnoreCase("unknown")));
    }

    @Test
    public void testUserDefinedSettings() {
        SettingKey userDefined = SettingKey.builder()
                .withName("user_defined")
                .withType(SettingType.UTF_8)
                .build();

        Properties props = new Properties();
        props.setProperty("user_defined", "haha");

        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder()
                .withProperties(props)
                .build();
        assertEquals("haha", cfg.settings().get(userDefined));
    }

    @Test
    void testRegionSetsRequiredSettings() {
        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder()
                .withSetting(SettingKey.region, "CN-NORTH-1")
                .build();
        assertTrue(cfg.secure());
        assertEquals(ByteHouseRegion.CN_NORTH_1.getHost(), cfg.host());
        assertEquals(ByteHouseRegion.CN_NORTH_1.getPort(), cfg.port());
    }

    @Test
    void testEmptyRegionDoesNotSetRequiredSettings() {
        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder()
                .host("localhost")
                .port(9000)
                .withSetting(SettingKey.region, "")
                .build();
        assertFalse(cfg.secure());
        assertEquals("localhost", cfg.host());
        assertEquals(9000, cfg.port());

        ByteHouseConfig cfg2 = ByteHouseConfig.Builder.builder()
                .host("localhost")
                .port(9000)
                .region("")
                .build();
        assertFalse(cfg2.secure());
        assertEquals("localhost", cfg2.host());
        assertEquals(9000, cfg2.port());
    }

    @Test
    void testRegionSetsRequiredSettingsFromConfig() {
        ByteHouseConfig existing = ByteHouseConfig.Builder.builder()
                .region("CN-NORTH-1")
                .host("somehost")
                .port(-1)
                .secure(false)
                .build();

        ByteHouseConfig cfg = ByteHouseConfig.Builder.builder(existing).build();

        assertEquals(ByteHouseRegion.CN_NORTH_1.getHost(), cfg.host());
        assertEquals(ByteHouseRegion.CN_NORTH_1.getPort(), cfg.port());
        assertTrue(cfg.secure());
    }
}
