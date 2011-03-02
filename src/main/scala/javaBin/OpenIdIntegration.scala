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

class OpenIdIntegration(endpointUrl: String, loginFormUrl: String) {
  /**
   * @see http://openid.net/specs/openid-authentication-1_1.html#anchor32
   */
  val contentType = ("Content-Type", "text/plain;charset=utf-8")

  val manager = new ServerManager() {
    setOPEndpointUrl(endpointUrl);
    // for a working demo, not enforcing RP realm discovery
    // since this new feature is not deployed
    getRealmVerifier().setEnforceRpId(false);
  }

  def processCheckidSetup(person: Person, request: ParameterList): Box[LiftResponse] = {
    val authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier())

    println("AuthReq")
    println("OPEndpoint =" + authReq.getOPEndpoint())
    println("Identity   =" + authReq.getIdentity())
    println("Claimed    =" + authReq.getClaimed())
    println("Handle     =" + authReq.getHandle())
    println("ReturnTo   =" + authReq.getReturnTo())
    println("Realm      =" + authReq.getRealm())

    val userSelectedClaimedId: String = null
    val opLocalId: String = null
    // if the user chose a different claimed_id than the one in request
    if (userSelectedClaimedId != null && userSelectedClaimedId.equals(authReq.getClaimed())) {
      //opLocalId = lookupLocalId(userSelectedClaimedId);
    }

    val response = manager.authResponse(request,
      opLocalId,
      userSelectedClaimedId,
      true,
      false);

    if (response.isInstanceOf[DirectError]) {
      return Full(InMemoryResponse(response.asInstanceOf[DirectError].keyValueFormEncoding.getBytes("utf-8"), Nil, Nil, 200))
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
    Full(RedirectResponse(response.getDestinationUrl(true)))

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

  def statelessDispatch(): LiftRules.DispatchPF = {
    // To be HTTP compliant return 200 OK on a HEAD request
    case req@Req(List("openid", "id"), _, HeadRequest) => () =>
      Full(InMemoryResponse(Array(), Nil, Nil, 200))
    case req@Req(List("openid", "id"), _, GetRequest) => () =>
      def generateXrds(url: String): Node =
        <xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
          <XRD>
            <Service priority="0">
              <Type>http://openid.net/signon/1.0</Type>
              <URI>{url}</URI>
            </Service>
          </XRD>
        </xrds:XRDS>

      val response = for {
          url <- Some(req.hostAndPath) if !url.equals("")
          xml = generateXrds(url + "/openid/login")
        } yield InMemoryResponse(xml.toString.getBytes("utf-8"), List(("Content-Type", "application/xrds+xml")), Nil, 200)
      Full(response.getOrElse(InternalServerErrorResponse()))
  }

  def openidMode(req: Req, mode: String): Boolean = {
    req.param("openid.mode").filter(mode ==).isDefined
  }

  def dispatch(): LiftRules.DispatchPF = {
    case req@Req(List("openid", "login"), _, PostRequest) if openidMode(req, "associate") => () =>
      println("openid/login: openid.mode=associate")
      val result = manager.associationResponse(req).keyValueFormEncoding()
      println("openid/login: openid.mode=associate. Result:")
      println(result)
      Full(InMemoryResponse(result.getBytes("utf-8"), Nil, Nil, 200))

    case req@Req(List("openid", "login"), _, PostRequest) if openidMode(req, "check_authentication") => () =>
      println("openid/login: openid.mode=check_authentication")
      val s = manager.verify(req).keyValueFormEncoding()
      println("openid/login: openid.mode=check_authentication. result:")
      println(s)
      Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))

    case req@Req(List("openid", "login"), _, GetRequest) if openidMode(req, "checkid_setup") => () =>
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
          Full(RedirectResponse(req.contextPath + loginFormUrl))
      }

    case req@Req(List("openid", "login"), _, method) => () =>
      println("openid/login: Unknown request: openid.mode=" + req.param("openid.mode").openOr("") + ", method=" + method.method)
      Full(NotFoundResponse())

    // This is where the form login redirects the user
    // Do not allow query parameters
    case req@Req(List("openid", "retry-login"), "", GetRequest) if req.request.queryString.isEmpty => () =>
      Person.currentUser match {
        case Full(person) =>
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
          Full(RedirectResponse(req.contextPath + loginFormUrl))
      }
  }

  def missingOpenIdSession() = {
    val msg = "No active OpenId session. Please go back to the application came from and try again."
    Full(InMemoryResponse(msg.getBytes("utf-8"), List(contentType), Nil, 404))
  }
}
