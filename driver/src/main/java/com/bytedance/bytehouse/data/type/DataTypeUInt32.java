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
package com.bytedance.bytehouse.data.type;

import com.bytedance.bytehouse.exception.ByteHouseSQLException;
import com.bytedance.bytehouse.misc.SQLLexer;
import com.bytedance.bytehouse.serde.BinaryDeserializer;
import com.bytedance.bytehouse.serde.BinarySerializer;
import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;

public class DataTypeUInt32 implements BaseDataTypeInt32<Long, Long> {

    @Override
    public String name() {
        return "UInt32";
    }

    @Override
    public Long defaultValue() {
        return 0L;
    }

    @Override
    public Class<Long> javaType() {
        return Long.class;
    }

    @Override
    public int getPrecision() {
        return 10;
    }

    @Override
    public void serializeBinary(Long data, BinarySerializer serializer) throws SQLException, IOException {
        serializer.writeInt(data.intValue());
    }

    @Override
    public Long deserializeBinary(BinaryDeserializer deserializer) throws SQLException, IOException {
        int res = deserializer.readInt();
        return 0xffffffffL & res;
    }

    @Override
    public Long convertJdbcToJavaType(Object obj, ZoneId tz) throws ByteHouseSQLException {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        throw new ByteHouseSQLException(-1, obj.getClass() + " cannot convert to " + Long.class);
    }

    @Override
    public Long deserializeText(SQLLexer lexer) throws SQLException {
        return lexer.numberLiteral().longValue();
    }

    @Override
    public Long[] deserializeBinaryBulk(int rows, BinaryDeserializer deserializer) throws IOException, SQLException {
        Long[] data = new Long[rows];
        for (int row = 0; row < rows; row++) {
            data[row] = this.deserializeBinary(deserializer);
        }
        return data;
    }

    @Override
    public Long[] allocate(int rows) {
        return new Long[rows];
    }
}