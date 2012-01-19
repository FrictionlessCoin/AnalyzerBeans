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
package org.eobjects.analyzer.result.renderer;

import org.eobjects.analyzer.beans.api.RendererBean;
import org.eobjects.analyzer.result.Crosstab;
import org.eobjects.analyzer.result.CrosstabResult;

@RendererBean(HtmlRenderingFormat.class)
public class CrosstabHtmlRenderer extends AbstractRenderer<CrosstabResult, String> {

	@Override
	public String render(CrosstabResult result) {
		return render(result.getCrosstab());
	}

	public String render(Crosstab<?> crosstab) {
		return new CrosstabRenderer(crosstab).render(new HtmlCrosstabRendererCallback());
	}

}
