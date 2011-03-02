package javaBin

import javaBin.model.Person
import net.liftweb.common.Full
import net.liftweb.http._
import org.openid4java.message._
import scala.collection.JavaConversions
import scala.xml._

object currentOpenIdRequest extends SessionVar[ParameterList]({error("fail")})

class OpenIdIntegration(endpointUrl: String) {
  val openIdIntegration = new OpenIdIntegrationTmp(endpointUrl)

  def generateXrds(url: String): Node =
    <xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
      <XRD>
        <Service priority="0">
          <Type>http://openid.net/signon/1.0</Type>
          <URI>{url}</URI>
        </Service>
      </XRD>
    </xrds:XRDS>

  def loginHtml(username: Option[String], message: Option[String]): NodeSeq =
    <html>
      <body>
        {message.getOrElse("")}
        <form action="/openid/do-login" method="POST">
          <table>
            <tr><td>Username</td><td><input id="username" name="username" value={username.getOrElse("")} type="text"/></td></tr>
            <tr><td>Password</td><td><input id="password" name="password" value="password" type="password"/></td></tr>
            <tr><td colspan="2"><input type="submit"/></td></tr>
            <tr><td colspan="2"><a href="/">Root</a></td></tr>
          </table>
        </form>
      </body>
    </html>

  def authenticate(username: String, password: String) = {
    username.equals("username") && password.equals("password")
  }

  implicit def reqToParameterList(req: Req): ParameterList = {
    val parameterMap: java.util.Map[String, Array[String]] = JavaConversions.asJavaMap(req.params.map((t:(String, List[String])) => (t._1, t._2.toArray)))
    new ParameterList(parameterMap)
  }

  def statelessDispatch(): LiftRules.DispatchPF = {
    case req@Req(List("openid", "id"), _, GetRequest) => () =>
      val response = for {
          url <- Some(req.hostAndPath) if !url.equals("")
          xml = generateXrds(url + "/openid/login")
        } yield InMemoryResponse(xml.toString.getBytes("utf-8"), List(("Content-Type", "application/xrds+xml")), Nil, 200)
      Full(response.getOrElse(InternalServerErrorResponse()))
  }

  def dispatch(): LiftRules.DispatchPF = {
    case req@Req(List("openid", "login"), _, PostRequest) if req.param("openid.mode").filter("associate".equals(_)).isDefined => () =>
      println("openid.mode=associate")
      val result = openIdIntegration.processAssociate(req)
      println("openid.mode=associate. Result:")
      println(result)
      Full(InMemoryResponse(result.getBytes("utf-8"), List(("Content-Type", "text/plain")), Nil, 200))

    case req@Req(List("openid", "login"), _, GetRequest) if req.param("openid.mode").filter("checkid_setup".equals(_)).isDefined => () =>
      val loggedIn = Person.loggedIn_?
      println("openid.mode=checkid_setup. logged in=" + loggedIn)

      if(loggedIn) {
        openIdIntegration.processCheckidSetup(currentOpenIdRequest.get, "navn navnesen", true, "a@b@c.om") match {
          case m if m.getClass.equals(classOf[DirectError]) =>
            val s = m.asInstanceOf[DirectError].keyValueFormEncoding
            println("openid.mode=checkid_setup. result:")
            println(s)
            Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))
          case m =>
            Full(RedirectResponse(m.getDestinationUrl(true)))
        }
      }
      else {
        // Save the request for later
        currentOpenIdRequest.set(req)

        Full(InMemoryResponse(loginHtml(None, None).toString.getBytes("utf-8"), List(("Content-Type", "text/html")), Nil, 200))
      }

    case req@Req(List("openid", "login"), _, PostRequest) if req.param("openid.mode").filter("check_authentication".equals(_)).isDefined => () =>
      println("openid.mode=check_authentication")
      val s = openIdIntegration.processCheckAuthentication(req).keyValueFormEncoding()
      println("openid.mode=check_authentication. result:")
      println(s)
      Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))

    // Do not allow query parameters
    case req@Req(List("openid", "do-login"), "", PostRequest) if req.request.queryString.isEmpty => () =>
      // TODO: Access only the form data, not query parameters
      val success: Boolean = (for {
        username <- req.params("username").headOption
        password <- req.params("password").headOption
      } yield authenticate(username, password)).getOrElse(false)

      println("do-login: success=" + success)
      println("do-login: openid request=" + currentOpenIdRequest.set_?)

      if (success) {
        openIdIntegration.processCheckidSetup(currentOpenIdRequest.get, "navn navnesen", true, "a@b@c.om") match {
          case m if m.getClass.equals(classOf[DirectError]) =>
            Full(InMemoryResponse(m.asInstanceOf[DirectError].keyValueFormEncoding.getBytes("utf-8"), Nil, Nil, 200))
          case m =>
            Full(RedirectResponse(m.getDestinationUrl(true)))
        }
      }
      else {
        Full(InMemoryResponse(loginHtml(req.params("username").headOption, Some("Login failed")).toString.getBytes("utf-8"), List(("Content-Type", "text/html")), Nil, 200))
      }
  }
}
