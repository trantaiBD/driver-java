package com.bytedance.bytehouse.data;

import com.bytedance.bytehouse.data.type.complex.DataTypeLowCardinality;
import com.bytedance.bytehouse.misc.BytesHelper;
import com.bytedance.bytehouse.serde.BinarySerializer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom Column class to be used with the Low Cardinality data type.
 */
public class ColumnLowCardinality extends AbstractColumn implements BytesHelper {
    private final static int HEADER_SIZE = 24; // defined by cnch protocol
    private final byte[] header;
    private final IColumn keys;
    private final Map<Object, Integer> objectToIndex;
    private final List<Integer> valueIndicesList;
    private byte[] valueIndicesRaw; // stores the keys to the values

    public ColumnLowCardinality(String name, DataTypeLowCardinality type, Object[] values) {
        super(name, type, values);

        header = new byte[HEADER_SIZE];
        keys = ColumnFactory.createColumn(null, type.getElemDataType(), null);
        objectToIndex = new HashMap<>();
        valueIndicesList = new ArrayList<>();
    }

    /**
     * Appends a row with Low Cardinality data to this column.
     * For LowCardinality serialization,
     * <br><br>
     * example: <br>
     * <p>
     * input:
     * <pre>
     * -----------------------------------
     * | col id | LowCardinality(String) |
     * -----------------------------------
     * |   0    | 'abc'                  |
     * -----------------------------------
     * |   1    | 'edf'                  |
     * -----------------------------------
     * |   2    | 'abc'                  |
     * -----------------------------------
     * </pre>
     * <p>
     * output:
     * <pre>
     *
     * header: [     .....        (16)-> 2 ]
     * Map(
     *    "abc" -> 0,
     *    "def" -> 1,
     * )
     * List(0,1,0)
     * </pre>
     */
    @Override
    public void write(Object object) throws IOException, SQLException {
        if (!objectToIndex.containsKey(object)) {
            objectToIndex.put(object, objectToIndex.size());
            keys.write(object);
        }

        valueIndicesList.add(objectToIndex.get(object));
    }


    /**
     * Flush data for all the rows to serializer.
     */
    @Override
    public void flushToSerializer(
            final BinarySerializer serializer,
            final boolean now
    ) throws SQLException {
        try {
            if (isExported()) {
                serializer.writeUTF8StringBinary(name);
                serializer.writeUTF8StringBinary(type.name());
            }

            updateHeader();
            updateValueIndicesRaw();

            // write values for all the rows
            serializer.writeBytes(header);
            keys.flushToSerializer(serializer, now);
            serializer.writeLong(valueIndicesList.size());
            serializer.writeBytes(valueIndicesRaw);
        } catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    private void updateValueIndicesRaw() {
        int rowNum = valueIndicesList.size();
        int idxSize = minIndexSize(rowNum);
        valueIndicesRaw = new byte[rowNum * idxSize];
        for (int i = 0; i < rowNum; i++) {
            putIndex(idxSize, valueIndicesList.get(i), i, valueIndicesRaw);
        }
    }

    private void putIndex(int idxSize, int idx, int row, byte[] memory) {
        switch (idxSize) {
            case 1:
                memory[row] = (byte) idx;
                break;
            case 2:
                setShortLE(memory, row * idxSize, idx);
                break;
            case 3:
                setIntLE(memory, row * idxSize, idx);
                break;
            case 4:
                setLongLE(memory, row * idxSize, idx);
                break;
            default:
                // Won't reach here since minIdxSize return a number only from 1-4
                throw new IllegalStateException("supposed unreachable execution path");
        }
    }

    private void updateHeader() {
        /* header[0] and header[9] are part of the CNCH protocol.
         More details from
         https://code.byted.org/dp/ClickHouse/blob/stable_v2/dbms/src/Columns/ColumnLowCardinality.cpp
        */
        header[0] = 1; // version
        header[9] = 2; // indicate presence of additional keys

        setLongLE(header, 16, objectToIndex.size());
        int idxSize = minIndexSize(objectToIndex.size());
        header[8] = (byte) (idxSize - 1);

    }

    /**
     * setColumnWriterBuffer() is called by Block to initialize the column for writing.
     */
    @Override
    public void setColumnWriterBuffer(ColumnWriterBuffer buffer) {
        super.setColumnWriterBuffer(buffer);
        keys.setColumnWriterBuffer(buffer);
    }

    @Override
    public void clear() {
        objectToIndex.clear();
        keys.clear();
    }

    private int minIndexSize(int n) {
        int indexSize = 0;

        if (n > (1 << 24)) {
            indexSize++;
        }
        if (n > (1 << 16)) {
            indexSize++;
        }
        if (n > (1 << 8)) {
            indexSize++;
        }

        return indexSize + 1;
    }


}
