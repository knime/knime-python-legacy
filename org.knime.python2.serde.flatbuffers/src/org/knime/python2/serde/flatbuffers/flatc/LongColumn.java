/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */

// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.python2.serde.flatbuffers.flatc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

@SuppressWarnings("javadoc")
public final class LongColumn extends Table {
    public static LongColumn getRootAsLongColumn(final ByteBuffer _bb) {
        return getRootAsLongColumn(_bb, new LongColumn());
    }

    public static LongColumn getRootAsLongColumn(final ByteBuffer _bb, final LongColumn obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb));
    }

    public void __init(final int _i, final ByteBuffer _bb) {
        bb_pos = _i;
        bb = _bb;
    }

    public LongColumn __assign(final int _i, final ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public long values(final int j) {
        final int o = __offset(4);
        return o != 0 ? bb.getLong(__vector(o) + (j * 8)) : 0;
    }

    public int valuesLength() {
        final int o = __offset(4);
        return o != 0 ? __vector_len(o) : 0;
    }

    public ByteBuffer valuesAsByteBuffer() {
        return __vector_as_bytebuffer(4, 8);
    }

    public boolean missing(final int j) {
        final int o = __offset(6);
        return o != 0 ? 0 != bb.get(__vector(o) + (j * 1)) : false;
    }

    public int missingLength() {
        final int o = __offset(6);
        return o != 0 ? __vector_len(o) : 0;
    }

    public ByteBuffer missingAsByteBuffer() {
        return __vector_as_bytebuffer(6, 1);
    }

    public static int createLongColumn(final FlatBufferBuilder builder, final int valuesOffset,
        final int missingOffset) {
        builder.startObject(2);
        LongColumn.addMissing(builder, missingOffset);
        LongColumn.addValues(builder, valuesOffset);
        return LongColumn.endLongColumn(builder);
    }

    public static void startLongColumn(final FlatBufferBuilder builder) {
        builder.startObject(2);
    }

    public static void addValues(final FlatBufferBuilder builder, final int valuesOffset) {
        builder.addOffset(0, valuesOffset, 0);
    }

    public static int createValuesVector(final FlatBufferBuilder builder, final long[] data) {
        builder.startVector(8, data.length, 8);
        for (int i = data.length - 1; i >= 0; i--) {
            builder.addLong(data[i]);
        }
        return builder.endVector();
    }

    public static void startValuesVector(final FlatBufferBuilder builder, final int numElems) {
        builder.startVector(8, numElems, 8);
    }

    public static void addMissing(final FlatBufferBuilder builder, final int missingOffset) {
        builder.addOffset(1, missingOffset, 0);
    }

    public static int createMissingVector(final FlatBufferBuilder builder, final boolean[] data) {
        builder.startVector(1, data.length, 1);
        for (int i = data.length - 1; i >= 0; i--) {
            builder.addBoolean(data[i]);
        }
        return builder.endVector();
    }

    public static void startMissingVector(final FlatBufferBuilder builder, final int numElems) {
        builder.startVector(1, numElems, 1);
    }

    public static int endLongColumn(final FlatBufferBuilder builder) {
        final int o = builder.endObject();
        return o;
    }
}
