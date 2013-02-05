package javaBin.util

import xml.{Node, Elem}

/**
 * @author Thor Åge Eldby (thoraageeldby@gmail.com)
 */
object XMLUtil {

  implicit def createElemWrapper(elem: Elem): ElemWrapper = new ElemWrapper(elem)

  class ElemWrapper(elem: Elem) {
    def addChild(newChild: Node) = elem match {
      case Elem(prefix, label, attribs, scope, child@_*) =>
        Elem(prefix, label, attribs, scope, child ++ newChild: _*)
      case _ => sys.error("Can only add children to elements!")
    }
  }
}
