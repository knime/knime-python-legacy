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
 *   Aug 14, 2019 (marcel): created
 */
package org.knime.python2.kernel;

import java.util.function.Consumer;

import org.knime.core.node.NodeLogger;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class PythonOutputLogger implements PythonOutputListener {

    private final Consumer<String> m_infoLogger;

    private final Consumer<String> m_warningLogger;

    private final Consumer<String> m_debugLogger;

    private static final String DEPRECATION_WARNING_SEARCH_STRING = "DeprecationWarning: ";

    private final boolean m_showDeprecationWarningAsInfo;

    private volatile boolean m_disabled = false;

    /**
     * Set up a PythonOutputLogger to log to the given {@link NodeLogger} instance.
     *
     * DeprecationWarnings from Python are downgraded to "info" messages in the {@link NodeLogger}.
     *
     * @param logger The {@link NodeLogger} to which to write the messages received from Python.
     */
    public PythonOutputLogger(final NodeLogger logger) {
        this(logger::info, logger::warn, logger::debug, true);
    }

    /**
     * Set up a PythonOutputLogger to log to the given consumers for different log levels.
     *
     * In contrast to the constructor receiving a {@link NodeLogger}, {@link PythonOutputLogger}s constructed like this
     * do not downgrade Python's DeprecationWarnings but show them as warnings.
     *
     * @param infoLogger The logger to which to write info messages received from Python.
     * @param warningLogger The logger to which to write warning messages received from Python.
     * @param debugLogger May be {@code null}. If not {@code null}, the logger to which to write all messages received
     *            from Python while this instance is {@link #setDisabled(boolean) disabled}.
     */
    public PythonOutputLogger(final Consumer<String> infoLogger, final Consumer<String> warningLogger,
        final Consumer<String> debugLogger) {
        this(infoLogger, warningLogger, debugLogger, false);
    }

    /**
     * Set up a PythonOutputLogger to log to the given consumers for different log levels. You need to specify whether
     * deprecation warnings should be shown as warnings or info.
     *
     * @param infoLogger The logger to which to write info messages received from Python.
     * @param warningLogger The logger to which to write warning messages received from Python.
     * @param debugLogger May be {@code null}. If not {@code null}, the logger to which to write all messages received
     *            from Python while this instance is {@link #setDisabled(boolean) disabled}.
     * @param showDeprecationWarningAsInfo If True, deprecation warnings will be downgraded to info when passed to the
     *            provided consumers. Otherwise they will be propagated as warnings.
     */
    public PythonOutputLogger(final Consumer<String> infoLogger, final Consumer<String> warningLogger,
        final Consumer<String> debugLogger, final boolean showDeprecationWarningAsInfo) {
        m_infoLogger = infoLogger;
        m_warningLogger = warningLogger;
        m_debugLogger = debugLogger;
        m_showDeprecationWarningAsInfo = showDeprecationWarningAsInfo;
    }

    @Override
    public void setDisabled(final boolean silenced) {
        m_disabled = silenced;
    }

    @Override
    public void messageReceived(final String message, final boolean isWarningMessage) {
        if (!m_disabled) {
            if (isWarningMessage) {
                if (m_showDeprecationWarningAsInfo && message.contains(DEPRECATION_WARNING_SEARCH_STRING)) {
                    m_infoLogger.accept(message);
                } else {
                    m_warningLogger.accept(message);
                }
            } else {
                m_infoLogger.accept(message);
            }
        } else if (m_debugLogger != null) {
            m_debugLogger.accept(message);
        }
    }
}
