package javaBin.snippet

import scala.xml.{Null, UnprefixedAttribute, Elem, NodeSeq}
import net.liftweb.http.CurrentReq

class AdminMenu {

  def links(xhtml: NodeSeq): NodeSeq = {
    val currentPage = CurrentReq.value.request.url.split("/").last
    for (node <- xhtml) yield {
      node match {
        case elem: Elem if currentPage == (elem \\ "a" \\ "@href").text =>
          elem % new UnprefixedAttribute("class", "active", Null)
        case _ =>
          node
      }
    }
  }
}
