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
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.python2.nodes.learner;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.python2.config.PythonExecutableSelectionPanel;
import org.knime.python2.config.PythonSourceCodeOptionsPanel;
import org.knime.python2.config.PythonSourceCodePanel;
import org.knime.python2.config.PythonVersionAndCommandConfig;
import org.knime.python2.config.PythonVersionAndExecutableSelectionPanel;
import org.knime.python2.generic.templates.SourceCodeTemplatesPanel;
import org.knime.python2.port.PickledObjectFile;
import org.knime.python2.prefs.PythonPreferences;

/**
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@Deprecated
final class PythonLearnerNodeDialog extends DataAwareNodeDialogPane {

    private final PythonSourceCodePanel m_sourceCodePanel;

    private final PythonSourceCodeOptionsPanel m_sourceCodeOptionsPanel;

    private final PythonExecutableSelectionPanel m_executableSelectionPanel;

    private final SourceCodeTemplatesPanel m_templatesPanel;

    public PythonLearnerNodeDialog() {
        m_sourceCodeOptionsPanel = new PythonSourceCodeOptionsPanel();
        m_executableSelectionPanel = new PythonVersionAndExecutableSelectionPanel(this,
            new PythonVersionAndCommandConfig(PythonPreferences.getPythonVersionPreference(),
                PythonPreferences::getCondaInstallationPath, PythonPreferences::getPython2CommandPreference,
                PythonPreferences::getPython3CommandPreference));
        m_sourceCodePanel = new PythonSourceCodePanel(this, PythonLearnerNodeConfig.getVariableNames(),
            m_sourceCodeOptionsPanel, m_executableSelectionPanel);
        m_templatesPanel = new SourceCodeTemplatesPanel(m_sourceCodePanel, "python-learner");
        addTab("Script", m_sourceCodePanel, false);
        addTab("Options", m_sourceCodeOptionsPanel);
        addTab(PythonExecutableSelectionPanel.DEFAULT_TAB_NAME, m_executableSelectionPanel);
        addTab("Templates", m_templatesPanel);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final PythonLearnerNodeConfig config = new PythonLearnerNodeConfig();
        config.loadFromInDialog(settings);
        m_sourceCodePanel.loadSettingsFrom(config, specs);
        m_sourceCodePanel.updateFlowVariables(
            getAvailableFlowVariables().values().toArray(new FlowVariable[getAvailableFlowVariables().size()]));
        m_sourceCodeOptionsPanel.loadSettingsFrom(config);
        m_executableSelectionPanel.loadSettingsFrom(settings);
        m_sourceCodePanel.updateData(new DataTableSpec[]{(DataTableSpec)specs[0]}, new BufferedDataTable[]{null},
            new PickledObjectFile[0]);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final BufferedDataTable[] input)
        throws NotConfigurableException {
        final BufferedDataTable inTable = input[0];
        final DataTableSpec inSpec = inTable != null ? inTable.getDataTableSpec() : new DataTableSpec();
        loadSettingsFrom(settings, new PortObjectSpec[]{inSpec});
        m_sourceCodePanel.updateData(new DataTableSpec[]{inSpec}, input, new PickledObjectFile[0]);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final PythonLearnerNodeConfig config = new PythonLearnerNodeConfig();
        m_sourceCodePanel.saveSettingsTo(config);
        m_sourceCodeOptionsPanel.saveSettingsTo(config);
        m_executableSelectionPanel.saveSettingsTo(settings);
        config.saveTo(settings);
    }

    @Override
    public boolean closeOnESC() {
        return false;
    }

    @Override
    public void onOpen() {
        m_sourceCodePanel.open();
    }

    @Override
    public void onClose() {
        m_sourceCodePanel.close();
    }
}
