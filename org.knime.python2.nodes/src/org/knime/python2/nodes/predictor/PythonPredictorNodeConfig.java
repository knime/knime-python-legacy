/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.python2.nodes.predictor;

import org.knime.code2.generic.VariableNames;
import org.knime.code2.python.PythonSourceCodeConfig;

class PythonPredictorNodeConfig extends PythonSourceCodeConfig {

	private static final VariableNames VARIABLE_NAMES = new VariableNames("flow_variables",
			new String[] { "input_table" }, new String[] { "output_table" }, null, new String[] { "input_model" }, null);

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDefaultSourceCode() {
		return "from pandas import Series\n" +
				"# Only use numeric columns\n" +
				"data = " + VARIABLE_NAMES.getInputTables()[0] + "._get_numeric_data()\n" +
				"# Use first column as value\n" +
				"value_column = data[data.columns[0]]\n" +
				"# List of predictions\n" +
				"predictions = []\n" +
				"# prediction = value * m + c\n" +
				"# m is first value in model\n" +
				"m = " + VARIABLE_NAMES.getInputObjects()[0] + "[0]\n" +
				"# c is second value in model\n" +
				"c = " + VARIABLE_NAMES.getInputObjects()[0] + "[1]\n" +
				"# Iterate over values\n" +
				"for i in range(len(value_column)):\n" +
				"\t# Calculate predictions\n" +
				"\tpredictions.append(value_column[i]*m+c)\n" +
				"# Copy input table to output table\n" +
				VARIABLE_NAMES.getOutputTables()[0] + " = " + VARIABLE_NAMES.getInputTables()[0] + ".copy()\n" +
				"# Append predictions\n" +
				VARIABLE_NAMES.getOutputTables()[0] + "['prediction'] = Series(predictions, index=" + VARIABLE_NAMES.getOutputTables()[0] + ".index)\n";
	}

	/**
	 * Get the variable names for this node
	 * 
	 * @return The variable names
	 */
	static VariableNames getVariableNames() {
		return VARIABLE_NAMES;
	}

}
