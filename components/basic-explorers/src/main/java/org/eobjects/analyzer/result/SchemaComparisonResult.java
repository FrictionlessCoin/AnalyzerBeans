/**
 * eobjects.org AnalyzerBeans
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.analyzer.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SchemaComparisonResult implements AnalyzerResult {

	private static final long serialVersionUID = 1L;

	private List<SchemaDifference<?>> schemaDifferences;
	private List<TableComparisonResult> tableComparisonResults;

	public SchemaComparisonResult(Collection<SchemaDifference<?>> schemaDifferences,
			Collection<TableComparisonResult> tableComparisonResults) {
		if (schemaDifferences == null) {
			throw new IllegalArgumentException("schemaDifferences cannot be null");
		}
		if (tableComparisonResults == null) {
			throw new IllegalArgumentException("tableComparisonResults cannot be null");
		}
		this.schemaDifferences = Collections.unmodifiableList(new ArrayList<SchemaDifference<?>>(schemaDifferences));
		this.tableComparisonResults = Collections.unmodifiableList(new ArrayList<TableComparisonResult>(
				tableComparisonResults));
	}

	public List<SchemaDifference<?>> getSchemaDifferences() {
		return schemaDifferences;
	}

	public List<TableComparisonResult> getTableComparisonResults() {
		return tableComparisonResults;
	}

	public boolean isSchemasEqual() {
		return schemaDifferences.isEmpty() && tableComparisonResults.isEmpty();
	}

	@Override
	public String toString() {
		return "SchemaComparisonResult[differences=" + schemaDifferences + ", table comparison="
				+ tableComparisonResults + "]";
	}
}
