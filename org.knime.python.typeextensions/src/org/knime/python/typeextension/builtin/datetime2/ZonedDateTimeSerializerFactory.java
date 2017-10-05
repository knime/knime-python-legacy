/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

package org.knime.python.typeextension.builtin.datetime2;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.python.typeextension.Serializer;
import org.knime.python.typeextension.SerializerFactory;

/**
 * Is used to serialize java8 ZonedTime objects to python datetime objects.
 * 
 * @author Clemens von Schwerin, KNIME.com, Konstanz, Germany
 */

public class ZonedDateTimeSerializerFactory extends SerializerFactory<ZonedDateTimeValue> {

    // public static final String SERIZALIZE_FORMAT = "yyyy-MM-dd
    // HH:mm:ss.SSSxxx";
    // public static final String DESERIALIZE_FORMAT = "yyyy-MM-dd
    // HH:mm:ss.SSSxxx'['VV']'";
    public static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSSxxx'['VV']'";

    public ZonedDateTimeSerializerFactory() {
        super(ZonedDateTimeValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializer<? extends ZonedDateTimeValue> createSerializer() {
        return new ZonedDateTimeSerializer();
    }

    private class ZonedDateTimeSerializer implements Serializer<ZonedDateTimeValue> {

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] serialize(ZonedDateTimeValue value) throws IOException {
            ZonedDateTime date = value.getZonedDateTime();
            /*
             * DateTimeFormatter formatter =
             * DateTimeFormatter.ofPattern(SERIZALIZE_FORMAT); String datestr =
             * date.format(formatter); datestr += "[" + date.getZone() + "]";
             */
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT);
            String datestr = date.format(formatter);
            return datestr.getBytes("UTF-8");
        }

    }

}