package org.eobjects.analyzer.beans.valuedist
import scala.collection.JavaConversions.collectionAsScalaIterable
import org.eobjects.analyzer.result.html.HeadElement
import org.eobjects.analyzer.result.html.HtmlRenderingContext
import org.eobjects.analyzer.result.ValueCountingAnalyzerResult
import org.eobjects.analyzer.util.LabelUtils
import org.eobjects.analyzer.result.ValueCount

class ValueDistributionChartScriptHeadElement(result: ValueCountingAnalyzerResult, chartElementId: String) extends HeadElement {

  override def toHtml(context: HtmlRenderingContext): String = {
    val valueCounts = result.getValueCounts();

    val unexpectedValueCount = result.getUnexpectedValueCount()
    if (unexpectedValueCount != null && unexpectedValueCount > 0) {
      valueCounts.add(new ValueCount(LabelUtils.UNEXPECTED_LABEL, unexpectedValueCount));
    }

    val uniqueCount = result.getUniqueCount();
    if (uniqueCount != null && uniqueCount > 0) {
      val vc = new ValueCount(LabelUtils.UNIQUE_LABEL, uniqueCount);
      valueCounts.add(vc);
    }

    // will be used to plot the y-axis value. Descending/negative because we want them to go from top to bottom.
    var negativeIndex = 0;

    return """<script type="text/javascript">
    //<![CDATA[
    var data = [
        """ +
      valueCounts.map(vc => {
        val color = getColor(vc);
        negativeIndex = negativeIndex - 1;
        "{label:\"" + escapeLabel(context, vc.getValue()) + "\", " + 
         "data:[[" + vc.getCount() + "," + negativeIndex + "]]" + 
         {if (color == null) "" else ", color:\"" + color + "\""} +
        "}" + "";
      }).mkString(",") + """
    ];
    draw_value_distribution_bar('""" + chartElementId + """', data, 2);
    //]]>
</script>
"""
  }
  
  def getColor(vc: ValueCount): String = {
    val v = vc.getValue();
    if (v == null) {
      return "#111";
    }
    v.toLowerCase() match {
      case "red"|"blue"|"green"|"yellow"|"orange"|"black" => return v.toLowerCase();
      case "not_processed" => return "#333";
      case LabelUtils.UNIQUE_LABEL => return "#ccc";
      case LabelUtils.BLANK_LABEL|"white" => return "#eee";
      case _ => return null;
    }
  }

  def escapeLabel(context: HtmlRenderingContext, value: AnyRef): String = {
    val escaped = context.escapeJson(LabelUtils.getValueLabel(value))
    return escaped.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }
}