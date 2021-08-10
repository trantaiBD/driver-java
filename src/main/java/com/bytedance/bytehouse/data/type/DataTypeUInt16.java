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

public class DataTypeUInt16 implements BaseDataTypeInt16<Integer, Integer> {

    @Override
    public String name() {
        return "UInt16";
    }

    @Override
    public Integer defaultValue() {
        return 0;
    }

    @Override
    public Class<Integer> javaType() {
        return Integer.class;
    }

    @Override
    public int getPrecision() {
        return 5;
    }

    @Override
    public void serializeBinary(Integer data, BinarySerializer serializer) throws SQLException, IOException {
        serializer.writeShort(data.shortValue());
    }

    @Override
    public Integer deserializeBinary(BinaryDeserializer deserializer) throws SQLException, IOException {
        short s = deserializer.readShort();
        return s & 0xffff;
    }

    @Override
    public Integer convertJdbcToJavaType(Object obj, ZoneId tz) throws ByteHouseSQLException {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        throw new ByteHouseSQLException(-1, obj.getClass() + " cannot convert to " + Integer.class);
    }

    @Override
    public Integer deserializeText(SQLLexer lexer) throws SQLException {
        return lexer.numberLiteral().intValue();
    }
}
