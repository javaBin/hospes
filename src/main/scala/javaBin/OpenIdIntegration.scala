package javaBin

import java.util.HashMap
import javaBin.model.Person
import net.liftweb.common._
import net.liftweb.http._
import org.openid4java.message._
import org.openid4java.message.ax._
import org.openid4java.server.ServerManager
import scala.collection.JavaConversions
import scala.xml._
import org.openid4java.message.sreg.{SRegResponse, SRegRequest, SRegMessage}

object currentOpenIdRequest extends SessionVar[Option[ParameterList]](None)

class OpenIdIntegration(endpointUrl: => String, loginUrl: => String, loginFormUrl: => String, openIdUrl: (Long) => String) {
  // This has to match the patterns as it should be used to build the endpoint and openIdUrls
  val xrdsPath = "/openid/id"

  /**
   * @see http://openid.net/specs/openid-authentication-1_1.html#anchor32
   */
  private val plainText = ("Content-Type", "text/plain;charset=utf-8")

  private val xrds = ("Content-Type", "application/xrds+xml")

  val manager = new ServerManager() {
    setOPEndpointUrl(endpointUrl);
    // for a working demo, not enforcing RP realm discovery
    // since this new feature is not deployed
    getRealmVerifier().setEnforceRpId(false);
  }

  def processCheckidSetup(person: Person, request: ParameterList): Box[LiftResponse] = {
    val authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier())

    val openIdUrl = this.openIdUrl(person.openIdKey.get)
    println("AuthReq")
    println("OPEndpoint = " + authReq.getOPEndpoint())
    println("Identity   = " + authReq.getIdentity())
    println("Claimed    = " + authReq.getClaimed())
    println("Handle     = " + authReq.getHandle())
    println("ReturnTo   = " + authReq.getReturnTo())
    println("Realm      = " + authReq.getRealm())

    println("OpenID URL = " + openIdUrl)

    val userSelClaimed: String = openIdUrl
    val userSelId: String = openIdUrl

    // if the user chose a different claimed_id than the one in request
//    if (userSelClaimed != null && userSelClaimed.equals(authReq.getClaimed())) {
//      userSelId = person.openIdUrl.get
//    }

    val response = manager.authResponse(request,
      userSelId,
      userSelClaimed,
      true,
      false);

    if (response.isInstanceOf[DirectError]) {
      val s = response.asInstanceOf[DirectError].keyValueFormEncoding
      println("result")
      println(s)
      return Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))
    }

    if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
      val ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
      if (ext.isInstanceOf[FetchRequest]) {
        val fetchReq = ext.asInstanceOf[FetchRequest];
        val required = fetchReq.getAttributes(true);
        //Map optional = fetchReq.getAttributes(false);
        if (required.containsKey("email")) {
          val userDataExt = new HashMap();
          //userDataExt.put("email", userData.get(3));

          val fetchResp = FetchResponse.createFetchResponse(fetchReq, userDataExt);
          // (alternatively) manually add attribute values
//          fetchResp.addAttribute("email", "http://schema.openid.net/contact/email", email);
          response.addExtension(fetchResp);
        }
      }
      else
        throw new UnsupportedOperationException("TODO");
    }

    if (authReq.hasExtension(SRegMessage.OPENID_NS_SREG)) {
      val ext = authReq.getExtension(SRegMessage.OPENID_NS_SREG);
      if (ext.isInstanceOf[SRegRequest]) {
        val sregReq = ext.asInstanceOf[SRegRequest];
        val required = sregReq.getAttributes(true);
        //List optional = sregReq.getAttributes(false);
        if (required.contains("email")) {
          // data released by the user
          val userDataSReg = new HashMap();
          //userData.put("email", "user@example.com");

          val sregResp = SRegResponse.createSRegResponse(sregReq, userDataSReg);
          // (alternatively) manually add attribute values
//          sregResp.addAttribute("email", email);
          response.addExtension(sregResp);
        }
      }
      else {
        throw new UnsupportedOperationException("TODO");
      }
    }

    // Sign the auth success message.
    // This is required as AuthSuccess.buildSignedList has a `todo' tag now.
    manager.sign(response.asInstanceOf[AuthSuccess]);

    // caller will need to decide which of the following to use:

    // option1: GET HTTP-redirect to the return_to URL
    val url = response.getDestinationUrl(true)
    println("result")
    println(url)
    Full(RedirectResponse(url))

    // option2: HTML FORM Redirection
    //RequestDispatcher dispatcher =
    //        getServletContext().getRequestDispatcher("formredirection.jsp");
    //httpReq.setAttribute("prameterMap", response.getParameterMap());
    //httpReq.setAttribute("destinationUrl", response.getDestinationUrl(false));
    //dispatcher.forward(request, response);
    //return null;
  }

  implicit def reqToParameterList(req: Req): ParameterList = {
    val parameterMap: java.util.Map[String, Array[String]] = JavaConversions.asJavaMap(req.params.map((t:(String, List[String])) => (t._1, t._2.toArray)))
    new ParameterList(parameterMap)
  }

  val generateXrds: Node =
    <xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
      <XRD>
        <Service priority="0">
          <Type>http://openid.net/signon/1.0</Type>
          <URI>{loginUrl}</URI>
        </Service>
      </XRD>
    </xrds:XRDS>

  def statelessDispatch(): LiftRules.DispatchPF = {
    // To be HTTP compliant return 200 OK on a HEAD request
    case req@Req(List("openid", "id"), "", method) => () => method match {
      case HeadRequest =>
        Full(InMemoryResponse(Array(), Nil, Nil, 200))
      case GetRequest =>
        val xml = generateXrds
        Full(InMemoryResponse(xml.toString.getBytes("utf-8"), List(xrds), Nil, 200))
      case _ =>
        Full(MethodNotAllowedResponse())
    }
    case req@Req(List("openid", "id", key), "", method) => () => method match {
      case HeadRequest =>
        Full(OkResponse())
      case GetRequest =>
        val xml = generateXrds
        Full(InMemoryResponse(xml.toString.getBytes("utf-8"), List(xrds), Nil, 200))
      case _ =>
        Full(MethodNotAllowedResponse())
    }
  }

  def openidMode(req: Req, mode: String): Boolean = {
    req.param("openid.mode").filter(mode ==).isDefined
  }

  def dispatch(): LiftRules.DispatchPF = {
    case req@Req(List("openid", "login"), "", PostRequest) if openidMode(req, "associate") => () =>
      println("openid/login: openid.mode=associate")
      val result = manager.associationResponse(req).keyValueFormEncoding()
      println("openid/login: openid.mode=associate. Result:")
      println(result)
      Full(InMemoryResponse(result.getBytes("utf-8"), Nil, Nil, 200))

    case req@Req(List("openid", "login"), "", PostRequest) if openidMode(req, "check_authentication") => () =>
      println("openid/login: openid.mode=check_authentication")
      val s = manager.verify(req).keyValueFormEncoding()
      println("openid/login: openid.mode=check_authentication. result:")
      println(s)
      Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))

    case req@Req(List("openid", "login"), fuck, GetRequest) if openidMode(req, "checkid_setup") => () =>
      println("fuck=" + fuck)
      Person.currentUser match {
        case Full(person) =>
          println("openid/login: openid.mode=checkid_setup. logged in=true")
          currentOpenIdRequest.set(None)
          processCheckidSetup(person, req)
        case _ =>
          println("openid/login: openid.mode=checkid_setup. logged in=false")
          // Save the request for later
          currentOpenIdRequest.set(Some(req))

          // And redirect to our login form
          println("loginFormUrl=" + loginFormUrl)
          Full(RedirectResponse(loginFormUrl))
      }

    case req@Req(List("openid", "login"), fuck, method) => () =>
      println("fuck=" + fuck)
      println("openid/login: Unknown request: openid.mode=" + req.param("openid.mode").openOr("") + ", method=" + method.method)
      Full(MethodNotAllowedResponse())

    // This is where the form login redirects the user
    // Do not allow query parameters
    case req@Req(List("openid", "retry-login"), "", GetRequest) if req.request.queryString.isEmpty => () =>
      Person.currentUser match {
        case Full(person) if person.openIdKey.get != 0 =>
          currentOpenIdRequest.get match {
            case Some(req) =>
              println("openid/retry-login: logged in=true, current openid request=true")
              currentOpenIdRequest.set(None)
              processCheckidSetup(person, req)
            case None =>
              println("openid/retry-login: logged in=true, current openid request=false")
              missingOpenIdSession
          }
        case _ =>
          println("openid/retry-login: logged in=false")
          // Save the request for later
          currentOpenIdRequest.set(Some(req))

          // And redirect to our login form
          Full(RedirectResponse(loginFormUrl))
      }

    case req@Req(List("openid", "retry-login"), "", method) => () =>
      println("openid/retry-login: Unknown request: method=" + method.method)
      Full(MethodNotAllowedResponse())
  }

  def missingOpenIdSession() = {
    val msg = "No active OpenId session. Please go back to the application came from and try again."
    Full(InMemoryResponse(msg.getBytes("utf-8"), List(plainText), Nil, 404))
  }
}
