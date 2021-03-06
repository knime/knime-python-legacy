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
package org.knime.python2.kernel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.ThreadPool;
import org.knime.python2.PythonCommand;
import org.knime.python2.generic.ImageContainer;
import org.knime.python2.generic.VariableNames;
import org.knime.python2.kernel.PythonKernelBackendRegistry.PythonKernelBackendType;
import org.knime.python2.kernel.messaging.PythonKernelResponseHandler;
import org.knime.python2.port.PickledObject;
import org.knime.python2.port.PickledObjectFile;

/**
 * Manages a python kernel including executing commands in separate threads and switching the underling kernel.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class PythonKernelManager implements AutoCloseable {

    private ThreadPool m_threadPool = new ThreadPool(8);

    private final PythonKernel m_kernel;

    private final List<PythonOutputListener> m_stdoutListeners = new ArrayList<>();

    private final List<PythonOutputListener> m_stderrListeners = new ArrayList<>();

    private final Object m_listVariablesLock = new Object();

    /**
     * Creates a manager that will start a new Python kernel using the {@link PythonKernelBackendType#PYTHON2} back end
     * and the given options.
     *
     * @param kernelOptions all configurable options
     *
     * @throws IOException If an exception occurred while starting the kernel.
     */
    public PythonKernelManager(final PythonKernelOptions kernelOptions) throws IOException {
        this(PythonKernelBackendType.PYTHON2, kernelOptions);
    }

    /**
     * Creates a manager that will start a new Python kernel using the given back end and options.
     *
     * @param kernelBackendType the identifier of the kernel back end to use
     * @param kernelOptions all configurable options
     *
     * @throws IOException If an exception occurred while starting the kernel.
     */
    public PythonKernelManager(final PythonKernelBackendType kernelBackendType, final PythonKernelOptions kernelOptions)
        throws IOException {
        final PythonCommand command = kernelOptions.getUsePython3() //
            ? kernelOptions.getPython3Command() //
            : kernelOptions.getPython2Command();
        try {
            m_kernel = PythonKernelQueue.getNextKernel(command, kernelBackendType, Collections.emptySet(),
                Collections.emptySet(), kernelOptions, PythonCancelable.NOT_CANCELABLE);
        } catch (final PythonCanceledExecutionException ex) {
            // Cannot happen. We pass a non-cancelable above.
            throw new IllegalStateException("Implementation error.", ex);
        }
    }

    /**
     * Returns the image with the given name.
     *
     * @param name Name of the image
     * @return The image
     * @throws IOException If an error occurred
     */
    public synchronized ImageContainer getImage(final String name) throws IOException {
        return m_kernel.getImage(name);
    }

    /**
     * Put a {@link PickledObject} into the python workspace (asynchronous).
     *
     * @param name the name of the variable in the python workspace
     * @param object the {@link PickledObject}
     * @param responseHandler the response handler
     *
     */
    public synchronized void putObject(final String name, final PickledObjectFile object,
        final PythonKernelResponseHandler<Void> responseHandler) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                Exception exception = null;
                try {
                    kernel.putObject(name, object);
                } catch (final Exception e) {
                    exception = e;
                }
                if (kernel.equals(m_kernel)) {
                    responseHandler.handleResponse(null, exception);
                }
            }
        });
    }

    /**
     * Get a {@link PickledObject} from the python workspace (asynchronous).
     *
     * @param name the name of the variable in the python workspace
     * @param file to pickle to
     * @param responseHandler the response handler
     */
    public synchronized void getObject(final String name, final File file,
        final PythonKernelResponseHandler<PickledObjectFile> responseHandler) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                PickledObjectFile response = null;
                Exception exception = null;
                try {
                    response = kernel.getObject(name, file, null);
                } catch (final Exception e) {
                    exception = e;
                }
                if (kernel.equals(m_kernel)) {
                    responseHandler.handleResponse(response, exception);
                }
            }
        });
    }

    /**
     * Execute the given source code.
     *
     * @param sourceCode The source code to execute
     * @param responseHandler Handler for the responded console output
     */
    public synchronized void execute(final String sourceCode,
        final PythonKernelResponseHandler<String[]> responseHandler) {
        executeInThread(kernel -> kernel.execute(sourceCode), responseHandler);
    }

    /**
     * Execute the given source code and check that all output ports are populated properly.
     *
     * @param sourceCode The source code to execute
     * @param responseHandler Handler for the responded console output
     */
    public synchronized void executeAndCheckOutputs(final String sourceCode,
        final PythonKernelResponseHandler<String[]> responseHandler) {
        executeInThread(kernel -> kernel.executeAndCheckOutputs(sourceCode), responseHandler);
    }

    private interface ExecutionFunction {
        String[] run(PythonKernel kernel) throws PythonIOException;
    }

    private void executeInThread(
        final ExecutionFunction executionCall,
        final PythonKernelResponseHandler<String[]> responseHandler) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                String[] response = null;
                Exception exception = null;
                try {
                    response = executionCall.run(kernel);
                } catch (final Exception e) {
                    exception = e;
                }
                if (kernel.equals(m_kernel)) {
                    responseHandler.handleResponse(response, exception);
                }
            }
        });
    }

    /**
     * Put the given flow variables into the workspace.
     *
     * @param name The name of the flow variables dict
     * @param flowVariables The flow variables
     * @param responseHandler Handler called after execution (response object is always null)
     */
    public synchronized void putFlowVariables(final String name, final Collection<FlowVariable> flowVariables,
        final PythonKernelResponseHandler<Void> responseHandler) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                Exception exception = null;
                try {
                    kernel.putFlowVariables(name, flowVariables);
                } catch (final Exception e) {
                    exception = e;
                }
                if (kernel.equals(m_kernel)) {
                    responseHandler.handleResponse(null, exception);
                }
            }
        });
    }

    /**
     * Put the given data into the python workspace.
     *
     * @param variableNames the variable names in the Python workspace for the data to put
     * @param flowVariables the flow variables to put
     * @param tables the tables to put
     * @param objects the objects to put
     * @param responseHandler handler called after execution (response object is always null)
     * @param executionMonitor an execution monitor for reporting progress
     * @param rowLimit the maximum number of rows to put into a single table chunk
     */
    public synchronized void putData(final VariableNames variableNames, final List<FlowVariable> flowVariables,
        final BufferedDataTable[] tables, final PickledObjectFile[] objects,
        final PythonKernelResponseHandler<Void> responseHandler, final ExecutionMonitor executionMonitor,
        final int rowLimit) {
        final PythonKernel kernel = m_kernel;
        runInThread(new PutDataRunnable(kernel, variableNames, flowVariables, tables, objects, responseHandler,
            executionMonitor, rowLimit));
    }

    /**
     * Get a {@link DataTable} from the workspace.
     *
     * @param name The name of the table to get
     * @param exec the calling node's execution context
     * @param responseHandler Handler for the responded result table
     * @param executionMonitor an execution monitor for reporting progress
     */
    public synchronized void getDataTable(final String name, final ExecutionContext exec,
        final PythonKernelResponseHandler<DataTable> responseHandler, final ExecutionMonitor executionMonitor) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                DataTable response = null;
                Exception exception = null;
                try {
                    response = kernel.getDataTable(name, exec, executionMonitor);
                } catch (final Exception e) {
                    exception = e;
                }
                if (kernel.equals(m_kernel)) {
                    responseHandler.handleResponse(response, exception);
                }
            }
        });
    }

    /**
     * Returns the list of all defined variables, functions, classes and loaded modules.
     *
     * Each variable map contains the fields 'name', 'type' and 'value'.
     *
     * @param responseHandler Handler for the responded list of variables
     */
    public synchronized void
        listVariables(final PythonKernelResponseHandler<List<Map<String, String>>> responseHandler) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                synchronized (m_listVariablesLock) {
                    List<Map<String, String>> response = null;
                    Exception exception = null;
                    try {
                        response = kernel.listVariables();
                    } catch (final Exception e) {
                        exception = e;
                    }
                    if (kernel.equals(m_kernel)) {
                        responseHandler.handleResponse(response, exception);
                    }
                }
            }
        });
    }

    /**
     * Returns the list of possible auto completions to the given source at the given position.
     *
     * Each auto completion contains the fields 'name', 'type' and 'doc'.
     *
     * @param sourceCode The source code
     * @param line Cursor position (line)
     * @param column Cursor position (column)
     * @param responseHandler Handler for the responded possible auto completions
     */
    public synchronized void autoComplete(final String sourceCode, final int line, final int column,
        final PythonKernelResponseHandler<List<Map<String, String>>> responseHandler) {
        final PythonKernel kernel = m_kernel;
        runInThread(new Runnable() {
            @Override
            public void run() {
                List<Map<String, String>> response = null;
                Exception exception = null;
                try {
                    response = kernel.autoComplete(sourceCode, line, column);
                } catch (final Exception e) {
                    exception = e;
                }
                if (kernel.equals(m_kernel)) {
                    responseHandler.handleResponse(response, exception);
                }
            }
        });
    }

    /**
     * Closes the underlying Python kernel.
     *
     * @throws IllegalStateException if an exception occurred while cleaning up the Python kernel
     */
    @Override
    public synchronized void close() {
        m_threadPool.shutdown();
        m_threadPool = new ThreadPool(8);
        try {
            m_kernel.close();
        } catch (PythonKernelCleanupException ex) {
            NodeLogger.getLogger(PythonKernelManager.class).error(ex.getMessage(), ex);
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    /**
     * Runs the given runnable in a separate thread.
     *
     * @param runnable The runnable to run
     */
    private void runInThread(final Runnable runnable) {
        try {
            m_threadPool.submit(runnable);
        } catch (final InterruptedException e) {
            //
        }
    }

    /**
     * Get the managed python kernel.
     *
     * @return the managed python kernel
     */
    public PythonKernel getKernel() {
        return m_kernel;
    }

    /**
     * Add a listener receiving live messages from the python stdout stream.
     *
     * @param listener a {@link PythonOutputListener}
     */
    public synchronized void addStdoutListener(final PythonOutputListener listener) {
        m_stdoutListeners.add(listener);
        m_kernel.addStdoutListener(listener);
    }

    /**
     * Add a listener receiving live messages from the python stderror stream.
     *
     * @param listener a {@link PythonOutputListener}
     */
    public synchronized void addStderrorListener(final PythonOutputListener listener) {
        m_stderrListeners.add(listener);
        m_kernel.addStderrorListener(listener);
    }

    /**
     * Remove a listener receiving live messages from the python stdout stream.
     *
     * @param listener a {@link PythonOutputListener}
     */
    public synchronized void removeStdoutListener(final PythonOutputListener listener) {
        m_stdoutListeners.remove(listener);
        m_kernel.removeStdoutListener(listener);
    }

    /**
     * Remove a listener receiving live messages from the python stderror stream.
     *
     * @param listener a {@link PythonOutputListener}
     */
    public synchronized void removeStderrorListener(final PythonOutputListener listener) {
        m_stderrListeners.remove(listener);
        m_kernel.removeStderrorListener(listener);
    }

    private final class PutDataRunnable implements Runnable {

        private final PythonKernel m_localKernel;

        private final VariableNames m_variableNames;

        private final Collection<FlowVariable> m_flowVariables;

        private final BufferedDataTable[] m_tables;

        private final PickledObjectFile[] m_objects;

        private final PythonKernelResponseHandler<Void> m_responseHandler;

        private final ExecutionMonitor m_executionMonitor;

        private final int m_rowLimit;

        private PutDataRunnable(final PythonKernel kernel, final VariableNames variableNames,
            final Collection<FlowVariable> flowVariables, final BufferedDataTable[] tables,
            final PickledObjectFile[] objects, final PythonKernelResponseHandler<Void> responseHandler,
            final ExecutionMonitor executionMonitor, final int rowLimit) {
            m_localKernel = kernel;
            m_variableNames = variableNames;
            m_flowVariables = flowVariables;
            m_tables = tables;
            m_objects = objects;
            m_responseHandler = responseHandler;
            m_executionMonitor = executionMonitor;
            m_rowLimit = rowLimit;
        }

        @Override
        public void run() {
            Exception exception = null;
            try {
                m_localKernel.putFlowVariables(m_variableNames.getFlowVariables(), m_flowVariables);
                final String[] inputObjectNames = m_variableNames.getInputObjects();
                for (int i = 0; i < m_objects.length; i++) {
                    final PickledObjectFile object = m_objects[i];
                    m_localKernel.putObject(inputObjectNames[i], object);
                }
                final String[] inputTableNames = m_variableNames.getInputTables();
                for (int i = 0; i < m_tables.length; i++) {
                    final BufferedDataTable table = m_tables[i];
                    final ExecutionMonitor monitor = m_executionMonitor.createSubProgress(1 / (double)m_tables.length);
                    m_localKernel.putDataTable(inputTableNames[i], table, monitor, m_rowLimit);
                }
                m_localKernel.setExpectedOutputTables(m_variableNames.getOutputTables());
                m_localKernel.setExpectedOutputImages(m_variableNames.getOutputImages());
                m_localKernel.setExpectedOutputObjects(m_variableNames.getOutputObjects());
            } catch (final Exception e) {
                exception = e;
            }
            if (m_localKernel.equals(m_kernel)) {
                m_responseHandler.handleResponse(null, exception);
            }
        }
    }
}
