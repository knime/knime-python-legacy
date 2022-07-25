/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 8, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python2.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Abstract implementation of a PythonSourceCodeBuilder.</br>
 * Only extend this class if you want to add custom behavior.
 * For all other purposes use {@link PythonSourceCodeBuilder} instead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <B> the concrete builder class
 */
public abstract class AbstractPythonSourceCodeBuilder<B extends AbstractPythonSourceCodeBuilder<B>> {

    private static final char NEW_LINE = '\n';

    private static final char TAB = '\t';

    private final StringBuilder m_sb = new StringBuilder();

    /**
     * Constructor.
     */
    protected AbstractPythonSourceCodeBuilder() {

    }

    /**
     * @return the string builder that stores the Python code
     */
    protected StringBuilder getInnerStringBuilder() {
        return m_sb;
    }

    // Elements:

    /**
     * Appends a boolean.
     *
     * @param b boolean to append
     * @return this builder
     */
    public B a(final boolean b) {
        a(toPython(b));
        return thisCasted();
    }

    /**
     * Appends a double.
     *
     * @param d double to append
     * @return this builder
     */
    public B a(final double d) {
        a(toPython(d));
        return thisCasted();
    }

    /**
     * Appends a float.
     *
     * @param f float to append
     * @return this builder
     */
    public B a(final float f) {
        a(toPython(f));
        return thisCasted();
    }

    /**
     * Appends an integer.
     *
     * @param i integer to append
     * @return this builder
     */
    public B a(final int i) {
        a(toPython(i));
        return thisCasted();
    }

    /**
     * Appends a long.
     *
     * @param l long to append
     * @return this builder
     */
    public B a(final long l) {
        a(toPython(l));
        return thisCasted();
    }

    /**
     * Puts the provided string into quotes and appends it.
     *
     * @param s string to quote and append
     * @return this builder
     */
    public B as(final String s) {
        m_sb.append(toPython(s));
        return thisCasted();
    }

    /**
     * Appends the provided string as Python formatted string i.e. puts it into quotes and prepends a 'f'.
     *
     * @param s to append as Python formatted string
     * @return this builder
     */
    public B asf(final String s) {
        m_sb.append(toPythonFormattedString(s));
        return thisCasted();
    }

    /**
     * Puts the provided string into quotes and prepends an 'r'.
     *
     * @param s string to append
     * @return this builder
     */
    public B asr(final String s) {
        m_sb.append(toPythonRawString(s));
        return thisCasted();
    }

    // Arrays:

    /**
     * Appends the provided boolean array.
     *
     * @param ba boolean array to append
     * @return this builder
     */
    public B a(final boolean[] ba) {
        a(toPython(ba));
        return thisCasted();
    }

    /**
     * Appends the provided double array.
     *
     * @param da double array to append
     * @return this builder
     */
    public B a(final double[] da) {
        a(toPython(da));
        return thisCasted();
    }

    /**
     * Appends the provided float array.
     *
     * @param fa float array to append
     * @return this builder
     */
    public B a(final float[] fa) {
        a(toPython(fa));
        return thisCasted();
    }

    /**
     * Appends the provided int array.
     *
     * @param ia int array to append
     * @return this builder
     */
    public B a(final int[] ia) {
        a(toPython(ia));
        return thisCasted();
    }

    /**
     * Appends the provided long array.
     *
     * @param la long array to append
     * @return this builder
     */
    public B a(final long[] la) {
        a(toPython(la));
        return thisCasted();
    }

    /**
     * Appends the provided string array.
     *
     * @param sa string array to append
     * @return this builder
     */
    public B as(final String[] sa) {
        m_sb.append(toPython(sa));
        return thisCasted();
    }

    /**
     * Appends the provided strings as formatted strings.
     *
     * @param sa string array to append
     * @return this builder
     */
    public B asf(final String[] sa) {
        m_sb.append(toPythonFormattedStringArray(sa));
        return thisCasted();
    }

    /**
     * Appends the provided strings as string arrays.
     *
     * @param sa string array to append
     * @return this builder
     */
    public B asr(final String[] sa) {
        m_sb.append(toPythonRawStringArray(sa));
        return thisCasted();
    }

    // Code:

    /**
     * Appends the provided code.
     *
     * @param code to append
     * @return this builder
     */
    public B a(final String code) {
        m_sb.append(code);
        return thisCasted();
    }

    /**
     * Appends a newline followed by the provided line of code.
     *
     * @param line of code to append
     * @return this builder
     */
    public B n(final String line) {
        n();
        m_sb.append(line);
        return thisCasted();
    }

    /**
     * Add the provided lines of code.
     *
     * @param lines of code to append
     * @return this builder
     */
    public B n(final String... lines) {
        n(Arrays.asList(lines));
        return thisCasted();
    }

    /**
     * Add the provided lines of code.
     *
     * @param lines of code to append
     * @return this builder
     */
    public B n(final Iterable<String> lines) {
        for (final String line : lines) {
            n(line);
        }
        return thisCasted();
    }

    /**
     * Applies the toString function to the provided lines and appends them.
     *
     * @param <T> type of objects representing lines
     * @param lines to append
     * @param toString function to turn the lines objects into strings
     * @return this builder
     */
    public <T> B n(final Iterable<T> lines, final Function<T, String> toString) {
        for (final T line : lines) {
            n(toString.apply(line));
        }
        return thisCasted();
    }

    /**
     * Appends a newline.
     *
     * @return this builder
     */
    public B n() {
        m_sb.append(NEW_LINE);
        return thisCasted();
    }

    /**
     * Appends a tab.
     *
     * @return this builder
     */
    public B t() {
        m_sb.append(TAB);
        return thisCasted();
    }

    @Override
    public String toString() {
        return m_sb.toString();
    }

    @SuppressWarnings("unchecked")
    private B thisCasted() {
        return (B)this;
    }

    /**
     * Representation of True in Python.
     */
    public static final String TRUE = "True";

    /**
     * Representation of False in Python.
     */
    public static final String FALSE = "False";

    /**
     * Representation of None in Python.
     */
    public static final String NONE = "None";

    /**
     * Representation of infinity in Python.
     */
    public static final String INFINITY = "inf";

    /**
     * Representation of NaN in Python.
     */
    public static final String NAN = "NaN";

    private static final char QUOTE = '"';

    private static final char[] FORMATTED_STRING_PREFIXES = new char[] { 'f', 'F' };
    private static final char[] RAW_STRING_PREFIXES = new char[] { 'r', 'R' };


    /**
     * @param b boolean to turn into string
     * @return the Python string representation of the provided boolean
     */
    public static String toPython(final boolean b) {//NOSONAR
        return b ? TRUE : FALSE;
    }

    /**
     * @param d double to turn into a Python string
     * @return the Python string representation of the provided double
     */
    public static String toPython(final double d) {
        if (Double.isInfinite(d)) {
            if (d > 0) {
                return INFINITY;
            } else {
                return "-" + INFINITY;
            }
        }
        if (Double.isNaN(d)) {
            return NAN;
        }
        return String.valueOf(d);
    }

    /**
     * @param f float to turn into a Python string
     * @return the Python string representation of the provided float
     */
    public static String toPython(final float f) {
        if (Float.isInfinite(f)) {
            if (f > 0) {
                return INFINITY;
            } else {
                return "-" + INFINITY;
            }
        }
        if (Float.isNaN(f)) {
            return NAN;
        }
        return String.valueOf(f);
    }

    /**
     * @param i int to turn into a Python string
     * @return the Python string representation of the provided int
     */
    public static String toPython(final int i) {
        return String.valueOf(i);
    }

    /**
     * @param l long to turn into a Python string
     * @return the Python string representation of the provided long
     */
    public static String toPython(final long l) {
        return String.valueOf(l);
    }

    /**
     * @param l long to turn into a Python string, will be turned into None if null
     * @return the Python string representation of the provided long
     */
    public static String toPython(final Long l) {
        return l == null ? NONE : toPython(l.longValue());
    }

    /**
     * @param f float to turn into a Python string, will be turned into None if null
     * @return the Python string representation of the provided float
     */
    public static String toPython(final Float f) {
        return f == null ? NONE : toPython(f.floatValue());
    }

    /**
     * @param d double to turn into a Python string, will be turned into None if null
     * @return the Python string representation of the provided double
     */
    public static String toPython(final Double d) {
        return d == null ? NONE : toPython(d.doubleValue());
    }

    /**
     * Quotes the provided string.
     *
     * @param s to quote for Python
     * @return the quoted string
     */
    public static String toPython(final String s) {
        // TODO: check if already in quotes etc.
        return QUOTE + s + QUOTE;
    }

    /**
     * Turns the provided string into a Python formatted string.
     *
     * @param s to process
     * @return the processed string
     */
    public static String toPythonFormattedString(final String s) {
        return FORMATTED_STRING_PREFIXES[0] + toPython(s);
    }

    /**
     * Turns the provided string into a Python raw string.
     *
     * @param s to process
     * @return the processed string
     */
    public static String toPythonRawString(final String s) {
        return RAW_STRING_PREFIXES[0] + toPython(s);
    }

    // Arrays:

    /**
     * Converts the provided boolean array to its Python representation.
     *
     * @param ba boolean array to process
     * @return the boolean array in Python representation
     */
    public static String toPython(final boolean[] ba) {
        final var str = new String[ba.length];
        for (int i = 0; i < str.length; i++) {//NOSONAR
            str[i] = toPython(ba[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts the provided double array to its Python representation.
     *
     * @param da double array to convert
     * @return the double array in Python representation
     */
    public static String toPython(final double[] da) {
        final var str = new String[da.length];
        for (int i = 0; i < str.length; i++) {//NOSONAR
            str[i] = toPython(da[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts the provided float array to its Python representation.
     *
     * @param fa float array to convert
     * @return the float array in Python representation
     */
    public static String toPython(final float[] fa) {
        final var str = new String[fa.length];
        for (int i = 0; i < str.length; i++) {//NOSONAR
            str[i] = toPython(fa[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts the provided int array to its Python representation.
     *
     * @param ia int array to convert
     * @return the int array in Python representation
     */
    public static String toPython(final int[] ia) {
        final var str = new String[ia.length];
        for (int i = 0; i < str.length; i++) {//NOSONAR
            str[i] = toPython(ia[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts the provided long array to its Python representation.
     *
     * @param la long array to convert
     * @return the long array in Python representation
     */
    public static String toPython(final long[] la) {
        final var str = new String[la.length];
        for (int i = 0; i < str.length; i++) {//NOSONAR
            str[i] = toPython(la[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts the provided Long array to its Python representation.
     * {@code null} is mapped to {@code None}.
     *
     * @param la Long array to convert
     * @return the Long array in Python representation
     */
    public static String toPython(final Long[] la) {
        return toPythonList(Arrays.stream(la).map(l -> l == null ? NONE : toPython(l)).toArray(String[]::new));
    }

    /**
     * Converts the provided Long matrix to its Python representation.
     * {@code null} is mapped to {@code None}.
     *
     * @param la Long matrix to convert
     * @return the Long matrix in Python representation
     */
    public static String toPython(final Long[][] la) {
        return toPythonList(Arrays.stream(la).map(AbstractPythonSourceCodeBuilder::toPython).toArray(String[]::new));
    }

    /**
     * Converts the provided String array to its Python representation.
     *
     * @param sa to convert
     * @return the string array in Python representation
     */
    public static String toPython(final String[] sa) {
        final var str = new String[sa.length];
        for (int i = 0; i < str.length; i++) {
            str[i] = toPython(sa[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts an OptionalLong to its Python representation.
     * {@link OptionalLong#empty()} is mapped to {@code None}.
     *
     * @param ol OptionalLong to convert
     * @return the OptionalLong in Python representation
     */
    public static String toPython(final OptionalLong ol) { //NOSONAR
        return ol.isPresent() ? toPython(ol.getAsLong()) : NONE;
    }

    /**
     * Converts an OptionalInt to its Python representation.
     * {@link OptionalInt#empty()} is mapped to {@code None}.
     * @param oi the OptionalInt to convert
     * @return the OpitonalInt in Python representation
     */
    public static String toPython(final OptionalInt oi) {//NOSONAR
        return oi.isPresent() ? toPython(oi.getAsInt()) : NONE;
    }

    /**
     * Converts an OptionalDouble to its Python representation.
     * {@link OptionalDouble#empty()} is mapped to {@code None}:
     *
     * @param od OptionalDouble to convert
     * @return the OptionalDouble in Python representation
     */
    public static String toPython(final OptionalDouble od) {//NOSONAR
        return od.isPresent() ? toPython(od.getAsDouble()) : NONE;
    }

    /**
     * Converts an Optional String to its Python representation.
     * {@link Optional#empty()} is mapped to {@code None}.
     *
     * @param os Optional<String> to convert
     * @return the Optional<String> in Python representation
     */
    public static String toPython(final Optional<String> os) {//NOSONAR
        return os.orElse(NONE);
    }

    /**
     * Converts an Optional to its Python representation by first applying the provided toString function.
     * {@link Optional#empty()} is mapped to {@code None}.
     *
     * @param <T> the type of object held by the Optional
     * @param oo the Optional to map to Python
     * @param toString the method that turns a T into a String
     * @return the Python representation of the Optional
     */
    public static <T> String toPython(final Optional<T> oo, final Function<T, String> toString) {//NOSONAR
        return toPython(oo.map(toString));
    }

    /**
     * Converts the provided String array into a String holding the array as Python formatted Strings.
     *
     * @param sa String array to convert
     * @return the Python representation
     */
    public static String toPythonFormattedStringArray(final String[] sa) {
        final var str = new String[sa.length];
        for (int i = 0; i < str.length; i++) {
            str[i] = toPythonFormattedString(sa[i]);
        }
        return toPythonList(str);
    }

    /**
     * Converts the provided String array into a String holding the array as Python raw Strings.
     *
     * @param sa String array to convert
     * @return the Python representation
     */
    public static String toPythonRawStringArray(final String[] sa) {
        final var str = new String[sa.length];
        for (int i = 0; i < str.length; i++) {
            str[i] = toPythonRawString(sa[i]);
        }
        return toPythonList(str);
    }

    private static String toPythonList(final String[] elements) {
        return elements == null ? NONE : join(elements, "[", "]");
    }

    private static String join(final String[] elements, final String prefix, final String suffix) {
        return Stream.of(elements).collect(Collectors.joining(",", prefix, suffix));
    }

    /**
     * Converts the provided Strings into a Python tuple.
     * If {@code elements} is {@code null}, the returned value is {@code None}.
     *
     * @param elements of the tuple
     * @return the tuple representation of elements
     */
    public static String toPythonTuple(final String[] elements) {
        if (elements == null) {
            return NONE;
        }
        return join(elements, "(", elements.length == 1 ? ",)" : ")");
    }
    /**
     * Converts the provided elements into a tuple.
     *
     * @param elements to convert
     * @return the elements as Python tuple
     */
    public static String toPythonTuple(final String elements) {
        return elements == null ? NONE : ("(" + elements + ")");
    }

}
