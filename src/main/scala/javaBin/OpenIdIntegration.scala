package javaBin

import java.util.HashMap
import javaBin.model.Person
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.mapper._
import org.openid4java.message._
import org.openid4java.message.ax._
import org.openid4java.server.ServerManager
import scala.collection.JavaConversions
import scala.xml._
import org.openid4java.message.sreg.{SRegResponse, SRegRequest, SRegMessage}

object currentOpenIdRequest extends SessionVar[ParameterList]({error("fail")})

class OpenIdIntegration(endpointUrl: String) {
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

  def generateXrds(url: String): Node =
    <xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
      <XRD>
        <Service priority="0">
          <Type>http://openid.net/signon/1.0</Type>
          <URI>{url}</URI>
        </Service>
      </XRD>
    </xrds:XRDS>

  def loginHtml(email: Option[String], message: Option[String]): NodeSeq =
    <html>
      <body>
        {message.getOrElse("")}
        <form action="/openid/do-login" method="POST">
          <table>
            <tr><td>Email</td><td><input id="email" name="email" value={email.getOrElse("")} type="text"/></td></tr>
            <tr><td>Password</td><td><input id="password" name="password" value="password" type="password"/></td></tr>
            <tr><td colspan="2"><input type="submit"/></td></tr>
            <tr><td colspan="2"><a href="/">Root</a></td></tr>
          </table>
        </form>
      </body>
    </html>

  def authenticate(email: String, password: String): Box[Person] = {
    return Person.find(By(Person.email, email))
  }

  implicit def reqToParameterList(req: Req): ParameterList = {
    val parameterMap: java.util.Map[String, Array[String]] = JavaConversions.asJavaMap(req.params.map((t:(String, List[String])) => (t._1, t._2.toArray)))
    new ParameterList(parameterMap)
  }

  def processCheckidSetup(request: ParameterList, userSelectedClaimedId: String, authenticatedAndApproved: Boolean, email: String): Message = {
      val authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier());

      val opLocalId: String = null;
      // if the user chose a different claimed_id than the one in request
      if (userSelectedClaimedId != null && userSelectedClaimedId.equals(authReq.getClaimed()))
      {
          //opLocalId = lookupLocalId(userSelectedClaimedId);
      }

      val response = manager.authResponse(request,
          opLocalId,
          userSelectedClaimedId,
          authenticatedAndApproved.booleanValue(),
          false);

      if (response.isInstanceOf[DirectError])
          return response; // directResponse(httpResp, response.keyValueFormEncoding())

      if (authReq.hasExtension(AxMessage.OPENID_NS_AX))
      {
          val ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
          if (ext.isInstanceOf[FetchRequest])
          {
              val fetchReq = ext.asInstanceOf[FetchRequest];
              val required = fetchReq.getAttributes(true);
              //Map optional = fetchReq.getAttributes(false);
              if (required.containsKey("email"))
              {
                  val userDataExt = new HashMap();
                  //userDataExt.put("email", userData.get(3));

                  val fetchResp = FetchResponse.createFetchResponse(fetchReq, userDataExt);
                  // (alternatively) manually add attribute values
                  fetchResp.addAttribute("email", "http://schema.openid.net/contact/email", email);
                  response.addExtension(fetchResp);
              }
          }
          else //if (ext instanceof StoreRequest)
          {
              throw new UnsupportedOperationException("TODO");
          }
      }

      if (authReq.hasExtension(SRegMessage.OPENID_NS_SREG))
      {
          val ext = authReq.getExtension(SRegMessage.OPENID_NS_SREG);
          if (ext.isInstanceOf[SRegRequest])
          {
              val sregReq = ext.asInstanceOf[SRegRequest];
              val required = sregReq.getAttributes(true);
              //List optional = sregReq.getAttributes(false);
              if (required.contains("email"))
              {
                  // data released by the user
                  val userDataSReg = new HashMap();
                  //userData.put("email", "user@example.com");

                  val sregResp = SRegResponse.createSRegResponse(sregReq, userDataSReg);
                  // (alternatively) manually add attribute values
                  sregResp.addAttribute("email", email);
                  response.addExtension(sregResp);
              }
          }
          else
          {
              throw new UnsupportedOperationException("TODO");
          }
      }

      // Sign the auth success message.
      // This is required as AuthSuccess.buildSignedList has a `todo' tag now.
      manager.sign(response.asInstanceOf[AuthSuccess]);

      // caller will need to decide which of the following to use:

      // option1: GET HTTP-redirect to the return_to URL
      return response;

      // option2: HTML FORM Redirection
      //RequestDispatcher dispatcher =
      //        getServletContext().getRequestDispatcher("formredirection.jsp");
      //httpReq.setAttribute("prameterMap", response.getParameterMap());
      //httpReq.setAttribute("destinationUrl", response.getDestinationUrl(false));
      //dispatcher.forward(request, response);
      //return null;
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
      val result = manager.associationResponse(req).keyValueFormEncoding()
      println("openid.mode=associate. Result:")
      println(result)
      Full(InMemoryResponse(result.getBytes("utf-8"), Nil, Nil, 200))

    case req@Req(List("openid", "login"), _, GetRequest) if req.param("openid.mode").filter("checkid_setup".equals(_)).isDefined => () =>
      val loggedIn = Person.loggedIn_?
      println("openid.mode=checkid_setup. logged in=" + loggedIn)

      if(loggedIn) {
        processCheckidSetup(currentOpenIdRequest.get, "navn navnesen", true, "a@b@c.om") match {
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
      val s = manager.verify(req).keyValueFormEncoding()
      println("openid.mode=check_authentication. result:")
      println(s)
      Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))

    // Do not allow query parameters
    case req@Req(List("openid", "do-login"), "", PostRequest) if req.request.queryString.isEmpty => () =>
      if(!currentOpenIdRequest.set_?) {
        Full(InMemoryResponse("No active OpenId session.".getBytes("utf-8"), List(contentType), Nil, 404))
      }
      else {
        val request = currentOpenIdRequest.get

        val person = for {
          email <- req.param("email")
          password <- req.param("password")
          person <- authenticate(email, password)
        } yield person

        person match {
          case Full(person) =>
            println("do-login: success=false")
            processCheckidSetup(request, person.firstName + " " + person.lastName, true, person.email) match {
              case m if m.getClass.equals(classOf[DirectError]) =>
                Full(InMemoryResponse(m.asInstanceOf[DirectError].keyValueFormEncoding.getBytes("utf-8"), Nil, Nil, 200))
              case m =>
                Full(RedirectResponse(m.getDestinationUrl(true)))
            }
          case _ =>
            println("do-login: success=false")
            val body = loginHtml(req.params("email").headOption, Some("Login failed"))
            Full(InMemoryResponse(body.toString.getBytes("utf-8"), List(("Content-Type", "text/html")), Nil, 200))
        }
      }
  }
}
