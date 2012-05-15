package javaBin

import javaBin.model.Person
import net.liftweb.common._
import net.liftweb.http._
import org.openid4java.message._
import org.openid4java.message.ax._
import org.openid4java.message.ax.FetchResponse._
import org.openid4java.message.sreg._
import org.openid4java.message.sreg.SRegResponse.createSRegResponse
import org.openid4java.server.ServerManager
import scala.collection.JavaConversions._
import scala.xml._
import net.liftweb.util.Helpers

object currentOpenIdRequest extends SessionVar[Option[ParameterList]](None)

object OpenIdIntegration {
  // This has to match the patterns
  val retryLoginPath = "/openid/retry-login"
}

class OpenIdIntegration(loginFormUrl: String) {

  /**
   * @see http://openid.net/specs/openid-authentication-1_1.html#anchor32
   */
  private val plainText = ("Content-Type", "text/plain;charset=utf-8")

  private val xrds = ("Content-Type", "application/xrds+xml")

  val manager = new ServerManager() {
    getRealmVerifier.setEnforceRpId(false)
  }

  def getEmail(person: Person) = Some(person.email.get).filter("" !=)
  def getName(person: Person) = for {
        first <- Some(person.firstName.get).filter("" !=)
        last <- Some(person.lastName.get).filter("" !=)
      } yield (first + " " + last)
  def getFirstName(person: Person) = Some(person.firstName.get).filter("" !=)
  def getLastName(person: Person) = Some(person.lastName.get).filter("" !=)

  def getAttribute(person: Person)(t: (String, String)): (String, Option[String]) = t._2 match {
    case "http://schema.openid.net/contact/email" => (t._1, getEmail(person))
    case "http://schema.openid.net/namePerson" => (t._1, getName(person))
    case "http://schema.openid.net/namePerson/first" => (t._1, getFirstName(person))
    case "http://schema.openid.net/namePerson/last" => (t._1, getLastName(person))

    case "http://axschema.org/contact/email" => (t._1, getEmail(person))
    case "http://axschema.org/namePerson" => (t._1, getName(person))
    case "http://axschema.org/namePerson/first" => (t._1, getFirstName(person))
    case "http://axschema.org/namePerson/last" => (t._1, getLastName(person))

    case "http://openid.net/schema/contact/email" => (t._1, getEmail(person))
    case "http://openid.net/schema/namePerson" => (t._1, getName(person))
    case "http://openid.net/schema/namePerson/first" => (t._1, getFirstName(person))
    case "http://openid.net/schema/namePerson/last" => (t._1, getLastName(person))

    case id => (id, None)
  }

  def processCheckidSetup(context: String, person: Person, request: ParameterList): Box[LiftResponse] = {
    val authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier)

    val openIdUrl = context + "/openid/id/" + person.openIdKey.get
    println("AuthReq")
    println("OPEndpoint = " + authReq.getOPEndpoint)
    println("Identity   = " + authReq.getIdentity)
    println("Claimed    = " + authReq.getClaimed)
    println("Handle     = " + authReq.getHandle)
    println("ReturnTo   = " + authReq.getReturnTo)
    println("Realm      = " + authReq.getRealm)

    val userSelId       = openIdUrl
    val userSelClaimed  = openIdUrl // authReq.getIdentity()
    println("userSelId      = " + userSelId)
    println("userSelClaimed = " + userSelClaimed)

    val response = manager.authResponse(request,
      userSelId,
      userSelClaimed,
      true,
      context + "/openid/id",
      false)

    if (response.isInstanceOf[DirectError]) {
      val s = response.asInstanceOf[DirectError].keyValueFormEncoding
      println("result")
      println(s)
      return Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))
    }

    if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
      val ext = authReq.getExtension(AxMessage.OPENID_NS_AX)
      if (ext.isInstanceOf[FetchRequest]) {
        val req = ext.asInstanceOf[FetchRequest]

        val requiredAttributes = mapAsScalaMap(req.getAttributes(true).asInstanceOf[java.util.Map[String, String]])
        val optionalAttributes = mapAsScalaMap(req.getAttributes(false).asInstanceOf[java.util.Map[String, String]])

        println("FetchRequest")
        println("Required:")
        requiredAttributes.map(_._1).foreach(println)
        println("Optional:")
        optionalAttributes.map(_._1).foreach(println)

        var requiredValues = requiredAttributes.map(getAttribute(person))
        val optionalValues = optionalAttributes.map(getAttribute(person))

        println("Required values")
        requiredValues.foreach(t => println(t._1 + "=>" + t._2))
        println("Optional values")
        optionalValues.foreach(t => println(t._1 + "=>" + t._2))

        val missingRequiredValues = requiredValues.filter(_._2.isEmpty).map(_._1)
        if(!missingRequiredValues.isEmpty) {
          println("Missing required values: " + missingRequiredValues)
          // TODO: figure out what to return here, for now just filter them out
          requiredValues = requiredValues.filter(_._2.isDefined)
        }

        val v2 = requiredValues.map(t => (t._1, t._2.get)) ++
              optionalValues.filter(_._2.isDefined).map(t => (t._1, t._2.get))
        response.addExtension(createFetchResponse(req, mutableMapAsJavaMap(v2)))
      }
      else {
        return Full(InternalServerErrorResponse())
      }
    }

    if (authReq.hasExtension(SRegMessage.OPENID_NS_SREG)) {
      val ext = authReq.getExtension(SRegMessage.OPENID_NS_SREG);
      if (ext.isInstanceOf[SRegRequest]) {
        val req = ext.asInstanceOf[SRegRequest];

        val requiredAttributes = mapAsScalaMap(req.getAttributes(true).asInstanceOf[java.util.Map[String, String]])
        val optionalAttributes = mapAsScalaMap(req.getAttributes(false).asInstanceOf[java.util.Map[String, String]])

        println("SRegRequest")
        println("Required:")
        println(requiredAttributes.map(_._1))
        println("Optional:")
        println(optionalAttributes.map(_._1))

        val requiredValues = requiredAttributes.map(getAttribute(person))
        val optionalValues = optionalAttributes.map(getAttribute(person))

        println("Required values")
        println(requiredValues)
        println("Optional values")
        println(optionalValues)

        val missingRequiredValues = requiredValues.filter(_._2.isEmpty).map(_._1)
        if(!missingRequiredValues.isEmpty) {
          println("Missing required values: " + missingRequiredValues)
          // TODO: figure out what to return here
        }

        val v2 = requiredValues.map(t => (t._1, t._2.get)) ++
              optionalValues.filter(_._2.isDefined).map(t => (t._1, t._2.get))
        response.addExtension(createSRegResponse(req, mutableMapAsJavaMap(v2)))
      }
      else {
        return Full(InternalServerErrorResponse())
      }
    }

    // Sign the auth success message.
    // This is required as AuthSuccess.buildSignedList has a `todo' tag now.
    manager.sign(response.asInstanceOf[AuthSuccess]);

    val url = response.getDestinationUrl(true)
    println("result")
    println(url)
    Full(RedirectResponse(url))
  }

  implicit def reqToParameterList(req: Req): ParameterList = {
    val parameterMap: java.util.Map[String, Array[String]] = mapAsJavaMap(req.params.map((t:(String, List[String])) => (t._1, t._2.toArray)))
    new ParameterList(parameterMap)
  }

  def generateOpIdentifierXrds(context: String): Node =
    <xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
      <XRD>
        <Service priority="0">
          <Type>http://specs.openid.net/auth/2.0/server</Type>
          <Type>{AxMessage.OPENID_NS_AX}</Type>
          <URI>{context + "/openid/id"}</URI>
        </Service>
      </XRD>
    </xrds:XRDS>

  def generateClaimedIdentifierXrds(context: String): Node =
    <xrds:XRDS xmlns:xrds="xri://$xrds" xmlns:openid="http://openid.net/xmlns/1.0" xmlns="xri://$xrd*($v*2.0)">
      <XRD>
        <Service priority="0">
          <Type>http://specs.openid.net/auth/2.0/signon</Type>
          <Type>{AxMessage.OPENID_NS_AX}</Type>
          <URI>{context + "/openid/id"}</URI>
        </Service>
      </XRD>
    </xrds:XRDS>

  def statelessDispatch(): LiftRules.DispatchPF = {
    case req@Req(List("openid", "id", key), "", method) => () => method match {
      case HeadRequest =>
        Full(OkResponse())
      case GetRequest =>
        val xml = generateClaimedIdentifierXrds(req.hostAndPath)
        Full(InMemoryResponse(xml.toString().getBytes("utf-8"), List(xrds), Nil, 200))
      case PostRequest =>
        println(method.method + " openid/id/" + key)
        println("query=" + req.request.queryString)
        req.headers.foreach(t => println(t._1 + ": " + t._2))
        val xml = generateClaimedIdentifierXrds(req.hostAndPath)
        Full(InMemoryResponse(xml.toString().getBytes("utf-8"), List(xrds), Nil, 200))
      case _ =>
        println("openid/id/" + key + ": Unknown request: method=" + method.method)
        println(method.method + " openid/id/" + key)
        println("query=" + req.request.queryString)
        req.headers.foreach(t => println(t._1 + ": " + t._2))
        Full(MethodNotAllowedResponse())
    }
  }

  def openidMode(req: Req, mode: String): Boolean = {
    req.param("openid.mode").filter(mode ==).isDefined
  }

  def checkidSetup(req: Req): Box[LiftResponse] = {
    val parameterList: ParameterList = req
    Person.currentUser match {
      case Full(person) =>
        println("openid/id: openid.mode=checkid_setup. logged in=true")
        println(parameterList)
        currentOpenIdRequest.set(None)
        processCheckidSetup(req.hostAndPath, person, req)
      case _ =>
        println("openid/id: openid.mode=checkid_setup. logged in=false")
        println(parameterList)
        // Save the request for later
        currentOpenIdRequest.set(Some(req))

        // And redirect to our login form
        Full(RedirectResponse(req.hostAndPath + loginFormUrl))
    }
  }

  def dispatch(): LiftRules.DispatchPF = {
    case req@Req(List("openid", "id"), "", method) => () =>
      req.param("openid.mode") match {
        case Full("associate") =>
          println("openid/id: openid.mode=associate")
          val parameterList: ParameterList = req
          println(parameterList)
          val result = manager.associationResponse(parameterList).keyValueFormEncoding()
          println("openid/id: openid.mode=associate. Result:")
          println(result)
          Full(InMemoryResponse(result.getBytes("utf-8"), Nil, Nil, 200))
        case Full("check_authentication") =>
          val parameterList: ParameterList = req
          println("openid/id: openid.mode=check_authentication")
          println(parameterList)
          val s = manager.verify(parameterList).keyValueFormEncoding()
          println("openid/id: openid.mode=check_authentication. result:")
          println(s)
          Full(InMemoryResponse(s.getBytes("utf-8"), Nil, Nil, 200))
        case Full("checkid_setup") =>
          checkidSetup(req)
        case _ =>
          req.header("Content-Type") match {
            case Full("application/x-www-form-urlencoded") =>
              println(method.method + " openid/xrds")
              println("query=" + req.request.queryString)
              req.headers.foreach(t => println(t._1 + ": " + t._2))
              println("req.body=")
              println(new String(Helpers.readWholeStream(req.request.inputStream)))
              println("openid/id: Unknown request: openid.mode=" + req.param("openid.mode").openOr("") + ", method=" + method.method)
              Full(MethodNotAllowedResponse())
            case _ =>
              val xml = generateClaimedIdentifierXrds(req.hostAndPath)
              Full(InMemoryResponse(xml.toString().getBytes("utf-8"), List(xrds), Nil, 200))
          }
      }

    // This is where the form login redirects the user
    // Do not allow query parameters
    case req@Req(List("openid", "retry-login"), "", GetRequest) if req.request.queryString.isEmpty => () =>
      Person.currentUser match {
        case Full(person) if person.openIdKey.get != 0 =>
          currentOpenIdRequest.get match {
            case Some(parameterList) =>
              println("openid/retry-login: logged in=true, current openid request=true")
              currentOpenIdRequest.set(None)
              processCheckidSetup(req.hostAndPath, person, parameterList)
            case None =>
              println("openid/retry-login: logged in=true, current openid request=false")
              missingOpenIdSession
          }
        case _ =>
          println("openid/retry-login: logged in=false")
          // Save the request for later
          currentOpenIdRequest.set(Some(req))

          // And redirect to our login form
          Full(RedirectResponse(req.hostAndPath + loginFormUrl))
      }

    case req@Req(List("openid", "retry-login"), "", method) => () =>
      println("openid/retry-login: Unknown request: method=" + method.method)
      Full(MethodNotAllowedResponse())
  }

  def missingOpenIdSession = {
    val msg = "No active OpenId session. Please go back to the application came from and try again."
    Full(InMemoryResponse(msg.getBytes("utf-8"), List(plainText), Nil, 404))
  }
}
