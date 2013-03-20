package org.eobjects.analyzer.beans.writers
import org.eobjects.analyzer.beans.api.Renderer
import org.eobjects.analyzer.result.html.HtmlFragment
import org.eobjects.analyzer.result.html.HtmlRenderer
import org.eobjects.analyzer.result.html.SimpleHtmlFragment
import scala.xml.PrettyPrinter
import javax.xml.transform.Transformer
import org.eobjects.analyzer.beans.api.RendererBean
import org.eobjects.analyzer.result.renderer.HtmlRenderingFormat
import org.eobjects.analyzer.beans.api.RendererPrecedence

@RendererBean(classOf[HtmlRenderingFormat])
class WriteDataResultHtmlRenderer extends Renderer[WriteDataResult, HtmlFragment] {
  
  override def getPrecedence(renderable: WriteDataResult) = RendererPrecedence.MEDIUM;

  override def render(r: WriteDataResult): HtmlFragment = {
    val inserts = r.getWrittenRowCount()
    val updates = r.getUpdatesCount()
    val errors = r.getErrorRowCount()

    val html = <div>
                 { if (inserts > 0) { <p>Executed { inserts } inserts</p> } }
                 { if (updates > 0) { <p>Executed { updates } updates</p> } }
                 { if (errors > 0) { <p>{ errors } Erroneous records</p> } }
               </div>;

    val frag = new SimpleHtmlFragment();
    frag.addBodyElement(html.toString());
    return frag;
  }
}