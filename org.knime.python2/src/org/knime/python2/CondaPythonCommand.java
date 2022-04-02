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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Conda-specific implementation of {@link PythonCommand}. Allows to build Python processes for a given Conda
 * installation and environment name. Takes care of resolving PATH-related issues on Windows.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class CondaPythonCommand extends AbstractCondaPythonCommand {

    private final String m_condaInstallationDirectoryPath;

    /**
     * Constructs a {@link PythonCommand} that describes a Python process of the given Python version that is run in the
     * Conda environment identified by the given Conda installation directory and the given Conda environment name.<br>
     * The validity of the given arguments is not tested.
     *
     * @param pythonVersion The version of Python environments launched by this command.
     * @param condaInstallationDirectoryPath The path to the directory of the Conda installation.
     * @param environmentDirectoryPath The path to the directory of the Conda environment. The directory does not
     *            necessarily need to be located inside the Conda installation directory, which is why a path is
     *            required.
     */
    public CondaPythonCommand(final PythonVersion pythonVersion, final String condaInstallationDirectoryPath,
        final String environmentDirectoryPath) {
        super(pythonVersion, environmentDirectoryPath);
        m_condaInstallationDirectoryPath = condaInstallationDirectoryPath;
    }

    @Override
    protected List<String> getAdditionalPathPrefixesForWindows() {
        // Note that we use the condabin directory of the given Conda installation directory regardless of whether
        // the given environment actually belongs to that Conda installation or a different instance. This is a
        // possible source of complications, but there is no known way to determine the environment's "correct"
        // Conda instance.
        var prefixes = new ArrayList<String>();
        prefixes.add(Paths.get(m_condaInstallationDirectoryPath, "condabin").toString());
        return prefixes;
    }
}
