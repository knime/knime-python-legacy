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
 *   May 6, 2020 (marcel): created
 */
package org.knime.python2;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.knime.conda.Conda;

/**
 * Conda-specific implementation of {@link PythonCommand}. Allows to build Python processes for a given Conda
 * installation and environment name. Takes care of resolving PATH-related issues on Windows.
 *
 * Abstract so that specialized classes can provide additional path prefixes for windows.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractCondaPythonCommand extends AbstractPythonCommand {

    private static final String PATH_ENVIRONMENT_VARIABLE_NAME = "PATH";

    private final String m_environmentDirectoryPath;

    /**
     * Note: path as in the PATH environment variable, not the path to the Python executable. Lazily initialized.
     */
    private List<String> m_pathPrefixes = null;

    /**
     * Constructs a {@link PythonCommand} that describes a Python process of the given Python version that is run in the
     * Conda environment identified given Conda environment directory.<br>
     * The validity of the given arguments is not tested.
     *
     * @param pythonVersion The version of Python environments launched by this command.
     * @param environmentDirectoryPath The path to the directory of the Conda environment.
     */
    public AbstractCondaPythonCommand(final PythonVersion pythonVersion, final String environmentDirectoryPath) {
        super(pythonVersion, Arrays.asList(createExecutableString(environmentDirectoryPath)));
        m_environmentDirectoryPath = environmentDirectoryPath;
    }

    private String getPathPrefixLazilyInitialized() {
        if (m_pathPrefixes == null) {
            m_pathPrefixes = getPathPrefixes();
        }
        return String.join(File.pathSeparator, m_pathPrefixes);
    }

    private List<String> getPathPrefixes() {
        final List<String> pathPrefixes = new ArrayList<>();
        // On Windows, we need to prepend a number of library paths to the PATH environment variable to resolve issues
        // that may occur when Python modules are searching for DLLs.
        if (SystemUtils.IS_OS_WINDOWS) {
            final var environmentDirectoryPath = getEnvironmentDirectoryPath();
            addToPrefixes(pathPrefixes, environmentDirectoryPath);
            addToPrefixes(pathPrefixes, environmentDirectoryPath, "Library", "mingw-w64", "bin");
            addToPrefixes(pathPrefixes, environmentDirectoryPath, "Library", "usr", "bin");
            addToPrefixes(pathPrefixes, environmentDirectoryPath, "Library", "bin");
            addToPrefixes(pathPrefixes, environmentDirectoryPath, "Scripts");
            addToPrefixes(pathPrefixes, environmentDirectoryPath, "bin");

            pathPrefixes.addAll(getAdditionalPathPrefixesForWindows());
        }
        return pathPrefixes;
    }

    /**
     * Construct the path variable prefix that should be used when executing Python. This method is called lazily, at
     * the first time when the path is needed.
     *
     * @return The path variable prefix or null if no path modification is needed.
     */
    protected abstract List<String> getAdditionalPathPrefixesForWindows();

    /**
     * Helper function to add components to a list of path prefixes
     *
     * @param prefixes current prefixes
     * @param first first part of the new path prefix
     * @param more remaining parts of the new path prefix
     */
    protected static void addToPrefixes(final List<String> prefixes, final String first, final String... more) {
        prefixes.add(Paths.get(first, more).toString());
    }

    /**
     * Paths are determined as per https://docs.anaconda.com/anaconda/user-guide/tasks/integration/python-path/
     */
    private static String createExecutableString(final String environmentDirectoryPath) {
        final Path executablePath;
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            // No Sonar: Path is supposed to be user configurable.
            executablePath = Paths.get(environmentDirectoryPath, "bin", "python"); // NOSONAR
        } else if (SystemUtils.IS_OS_WINDOWS) {
            // No Sonar: Path is supposed to be user configurable.
            executablePath = Paths.get(environmentDirectoryPath, "python.exe"); // NOSONAR
        } else {
            throw Conda.createUnknownOSException();
        }
        return executablePath.toString();
    }

    /**
     * @return The path to the directory of the Conda environment that is used by this instance.
     */
    public String getEnvironmentDirectoryPath() {
        return m_environmentDirectoryPath;
    }

    @Override
    public ProcessBuilder createProcessBuilder() {
        final ProcessBuilder pb = super.createProcessBuilder();
        final var pathVariablePrefix = getPathPrefixLazilyInitialized();
        if (pathVariablePrefix != null) {
            final String pathVariableName = findPathVariableName(pb);
            pb.environment().merge(pathVariableName, pathVariablePrefix,
                (oldPath, pathPrefix) -> pathPrefix + File.pathSeparatorChar + oldPath);
        }
        return pb;
    }

    /**
     * On Windows, the exact spelling/capitalization of the PATH variable name cannot be known in advance (and actually
     * should not matter since Windows is case-insensitive; Java does not seem to respect this fact, though). That is
     * why we have to scan the environment for the variable name and just pick whatever version of it is present.
     */
    private static String findPathVariableName(final ProcessBuilder pb) {
        String pathVariableName = PATH_ENVIRONMENT_VARIABLE_NAME;
        if (SystemUtils.IS_OS_WINDOWS) {
            for (final String variableName : pb.environment().keySet()) {
                if (PATH_ENVIRONMENT_VARIABLE_NAME.equalsIgnoreCase(variableName)) {
                    pathVariableName = variableName;
                    break;
                }
            }
        }
        return pathVariableName;
    }
}
