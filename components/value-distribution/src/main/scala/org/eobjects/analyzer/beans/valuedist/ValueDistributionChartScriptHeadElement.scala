package org.eobjects.analyzer.beans.valuedist
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import org.eobjects.analyzer.result.html.HeadElement
import org.eobjects.analyzer.result.html.HtmlRenderingContext

class ValueDistributionChartScriptHeadElement(result: ValueDistributionGroupResult, chartElementId: String) extends HeadElement {

  override def toHtml(context: HtmlRenderingContext): String = {
    val valueCounts = result.getTopValues().getValueCounts() ++ result.getBottomValues().getValueCounts()
    if (result.getNullCount() > 0) {
      valueCounts.add(new ValueCount("<null>", result.getNullCount()));
    }
    if (result.getUniqueCount() > 0) {
      valueCounts.add(new ValueCount("<unique>", result.getUniqueCount()));
    }

    return """<script type="text/javascript"><!--
   google.setOnLoadCallback(function() {
     var elem = document.getElementById("""" + chartElementId + """");
     var data = google.visualization.arrayToDataTable([
     ['Value', 'Count'],""" +
      valueCounts.map(vc => {
        "['" + context.escapeJson(vc.getValue()) + "', " + vc.getCount() + "]";
      }).mkString(",") + """
     ]);
     
     var chart = new google.visualization.PieChart(elem);
     
     wait_for_script_load('jQuery', function() {
       var options = {"width": $(elem).width(), "height": $(elem).height()};
       chart.draw(data, options);
     });
   });
--></script>"""
  }
}