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
package org.knime.python2.nodes.learner2;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.python2.kernel.PythonExecutionMonitorCancelable;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.nodes.PythonNodeModel;
import org.knime.python2.port.PickledObjectFileStorePortObject;
import org.knime.python2.ports.PickledObjectOutputPort;

/**
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class PythonLearnerNodeModel2 extends PythonNodeModel<PythonLearnerNodeConfig2> {

    private final PickledObjectOutputPort m_pickledObjectPort =
        new PickledObjectOutputPort(PythonLearnerNodeConfig2.getVariableNames().getOutputObjects()[0]);

    protected PythonLearnerNodeModel2() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{PickledObjectFileStorePortObject.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        PickledObjectFileStorePortObject portObject = null;
        final PythonExecutionMonitorCancelable cancelable = new PythonExecutionMonitorCancelable(exec);
        try (final PythonKernel kernel = getNextKernelFromQueue(cancelable)) {
            kernel.putFlowVariables(PythonLearnerNodeConfig2.getVariableNames().getFlowVariables(),
                getAvailableFlowVariables().values());
            kernel.putDataTable(PythonLearnerNodeConfig2.getVariableNames().getInputTables()[0],
                (BufferedDataTable)inData[0], exec.createSubProgress(0.3));
            final String[] output =
                kernel.execute(getConfig().getSourceCode(), cancelable);
            setExternalOutput(new LinkedList<>(Arrays.asList(output[0].split("\n"))));
            setExternalErrorOutput(new LinkedList<>(Arrays.asList(output[1].split("\n"))));
            exec.createSubProgress(0.6).setProgress(1);
            final Collection<FlowVariable> variables =
                kernel.getFlowVariables(PythonLearnerNodeConfig2.getVariableNames().getFlowVariables());
            portObject = m_pickledObjectPort.execute(kernel, exec);
            exec.createSubProgress(0.1).setProgress(1);
            addNewVariables(variables);
            shutdownKernel(kernel);
        }
        return new PortObject[]{portObject};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{null};
    }

    @Override
    protected PythonLearnerNodeConfig2 createConfig() {
        return new PythonLearnerNodeConfig2();
    }
}
