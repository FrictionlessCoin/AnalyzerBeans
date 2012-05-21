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
package org.eobjects.analyzer.result.html;

import java.util.List;

import org.eobjects.analyzer.beans.api.Renderer;
import org.eobjects.analyzer.descriptors.DescriptorProvider;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.renderer.HtmlRenderingFormat;
import org.eobjects.analyzer.result.renderer.RendererFactory;

/**
 * Body element to be included when facilitating "Drill to detail" behaviour.
 * Use this in conjunction with the {@link DrillToDetailsHeadElement}.
 */
public class DrillToDetailsBodyElement implements BodyElement {

    private final DescriptorProvider _descriptorProvider;
    private final AnalyzerResult _result;
    private final String _elementId;

    public DrillToDetailsBodyElement(String elementId, DescriptorProvider descriptorProvider, AnalyzerResult result) {
        if (descriptorProvider == null) {
            throw new IllegalArgumentException("DescriptorProvider cannot be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("AnalyzerResult cannot be null");
        }
        _elementId = elementId;
        _descriptorProvider = descriptorProvider;
        _result = result;
    }

    @Override
    public String toHtml() {
        final RendererFactory rendererFactory = new RendererFactory(_descriptorProvider);
        final Renderer<? super AnalyzerResult, ? extends HtmlFragment> renderer = rendererFactory.getRenderer(_result,
                HtmlRenderingFormat.class);
        if (renderer == null) {
            throw new IllegalStateException("No renderer found for: " + _result);
        }

        final HtmlFragment htmlFragment = renderer.render(_result);

        final StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"" + _elementId + "\" style=\"display:none;\">\n");

        // Append head elements inline (because we cannot reach the head
        // elements from here)
        final List<HeadElement> headElements = htmlFragment.getHeadElements();
        for (HeadElement headElement : headElements) {
            sb.append(headElement);
            sb.append('\n');
        }

        // Append body elements
        final List<BodyElement> bodyElements = htmlFragment.getBodyElements();
        for (BodyElement bodyElement : bodyElements) {
            sb.append(bodyElement.toHtml());
            sb.append('\n');
        }

        sb.append("</div>");
        return sb.toString();
    }

}
