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
package com.bytedance.bytehouse.data.type.complex;

import com.bytedance.bytehouse.data.IDataType;
import com.bytedance.bytehouse.exception.ByteHouseSQLException;
import com.bytedance.bytehouse.misc.BytesCharSeq;
import com.bytedance.bytehouse.misc.SQLLexer;
import com.bytedance.bytehouse.serde.BinaryDeserializer;
import com.bytedance.bytehouse.serde.BinarySerializer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneId;

public class DataTypeString implements IDataType<CharSequence, String> {

    public static DataTypeCreator<CharSequence, String> CREATOR = (lexer, serverContext) -> new DataTypeString(serverContext.getConfigure().charset());

    private final Charset charset;

    public DataTypeString(Charset charset) {
        this.charset = charset;
    }

    @Override
    public String name() {
        return "String";
    }

    @Override
    public int sqlTypeId() {
        return Types.VARCHAR;
    }

    @Override
    public String defaultValue() {
        return "";
    }

    @Override
    public Class<CharSequence> javaType() {
        return CharSequence.class;
    }

    @Override
    public Class<String> jdbcJavaType() {
        return String.class;
    }

    @Override
    public int getPrecision() {
        return 0;
    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public void serializeBinary(CharSequence data, BinarySerializer serializer) throws SQLException, IOException {
        if (data instanceof BytesCharSeq) {
            serializer.writeBytesBinary(((BytesCharSeq) data).bytes());
        } else {
            serializer.writeStringBinary(data.toString(), charset);
        }
    }

    /**
     * deserializeBinary will always returns String
     * for getBytes(idx) method, we encode the String again
     */
    @Override
    public String deserializeBinary(BinaryDeserializer deserializer) throws SQLException, IOException {
        byte[] bs = deserializer.readBytesBinary();
        return new String(bs, charset);
    }

    @Override
    public CharSequence convertJdbcToJavaType(Object obj, ZoneId tz) throws ByteHouseSQLException {
        if (obj instanceof CharSequence) {
            return (CharSequence) obj;
        }
        if (obj instanceof byte[]) {
            return new BytesCharSeq((byte[]) obj);
        }
        return obj.toString();
    }

    @Override
    public CharSequence deserializeText(SQLLexer lexer) throws SQLException {
        return lexer.stringView();
    }

    @Override
    public String[] getAliases() {
        return new String[]{
                "LONGBLOB",
                "MEDIUMBLOB",
                "TINYBLOB",
                "MEDIUMTEXT",
                "CHAR",
                "VARCHAR",
                "TEXT",
                "TINYTEXT",
                "LONGTEXT",
                "BLOB"};
    }
}
