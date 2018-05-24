package controllers

import java.io._
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.util.zip.GZIPInputStream
import java.util.{Base64, Date}

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.script.{Invocable, ScriptEngineManager}
import javax.xml.bind.DatatypeConverter
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.io.Source

class MkmController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  def call(apiCallData: ApiCallData): String = {
    val mkmAppToken = apiCallData.appToken
    val mkmAppSecret = apiCallData.appSecret
    val mkmAccessToken = apiCallData.accessToken
    val mkmAccessTokenSecret = apiCallData.accessTokenSecret
    val app = new M11DedicatedApp(mkmAppToken, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret)
    app.setDebug(true)
    if (app.request("https://api.cardmarket.com/ws/v2.0/output.json/account")) {
      System.out.println("yes:\n" + app.responseContent)
      if (app.request("https://www.mkmapi.eu/ws/v2.0/output.json/stock/file")) {

        // https://stackoverflow.com/a/42106801/773842
        val engine = new ScriptEngineManager().getEngineByName("nashorn")
        engine.eval("var fun = function(raw) {return JSON.parse(raw).stock}")
        val invocable = engine.asInstanceOf[Invocable]
        val result = invocable.invokeFunction("fun", app.responseContent)

        // get string content from base64'd gzip
        val arr = Base64.getDecoder.decode(result.toString)
        val asd = new ByteArrayInputStream(arr)
        val gz = new GZIPInputStream(asd)
        //val rd=new BufferedReader(new InputStreamReader(gz))
        val cont = Source.fromInputStream(gz).mkString

        return cont
      }
    }
    sys.error(app.responseCode + ": " + app.responseContent)
  }
}


class M11DedicatedApp(var _mkmAppToken: String, var _mkmAppSecret: String, var _mkmAccessToken: String, var _mkmAccessTokenSecret: String) {
  private var _lastError: Throwable = null
  private var _lastCode = 0
  private var _lastContent: String = null
  private var _debug = false

  /**
    * Activates the console debug messages
    *
    * @param flag true if you want to enable console messages; false to disable any notification.
    */
  def setDebug(flag: Boolean): Unit = {
    _debug = flag
  }

  /**
    * Encoding function. To avoid deprecated version, the encoding used is UTF-8.
    */
  @throws[UnsupportedEncodingException]
  private def rawurlencode(str: String) = URLEncoder.encode(str, "UTF-8")

  private def _debug(msg: String): Unit = {
    if (_debug) {
      System.out.print(new Date())
      System.out.print(" > ")
      System.out.println(msg)
    }
  }

  /**
    * Get last Error exception.
    *
    * @return null if no errors; instead the raised exception.
    */
  def lastError: Throwable = _lastError

  /**
    * Perform the request to given url with OAuth 1.0a API.
    *
    * @param requestURL url to be requested. Ex. https://www.mkmapi.eu/ws/v1.1/products/island/1/1/false
    * @return true if request was successfully executed. You can retrieve the content with responseContent();
    */
  def request(requestURL: String): Boolean = {
    _lastError = null
    _lastCode = 0
    _lastContent = null: String
    try {
      _debug("Requesting " + requestURL)
      val realm = requestURL
      val oauth_version = "1.0"
      val oauth_consumer_key = _mkmAppToken
      val oauth_token = _mkmAccessToken
      val oauth_signature_method = "HMAC-SHA1"
      // String oauth_timestamp = "" + (System.currentTimeMillis()/1000) ;
      val oauth_timestamp = "1407917892"
      // String oauth_nonce = "" + System.currentTimeMillis() ;
      val oauth_nonce = "53eb1f44909d6"
      val encodedRequestURL = rawurlencode(requestURL)
      var baseString = "GET&" + encodedRequestURL + "&"
      val paramString = "oauth_consumer_key=" + rawurlencode(oauth_consumer_key) + "&" + "oauth_nonce=" + rawurlencode(oauth_nonce) + "&" + "oauth_signature_method=" + rawurlencode(oauth_signature_method) + "&" + "oauth_timestamp=" + rawurlencode(oauth_timestamp) + "&" + "oauth_token=" + rawurlencode(oauth_token) + "&" + "oauth_version=" + rawurlencode(oauth_version)
      baseString = baseString + rawurlencode(paramString)
      val signingKey = rawurlencode(_mkmAppSecret) + "&" + rawurlencode(_mkmAccessTokenSecret)
      val mac = Mac.getInstance("HmacSHA1")
      val secret = new SecretKeySpec(signingKey.getBytes, mac.getAlgorithm)
      mac.init(secret)
      val digest = mac.doFinal(baseString.getBytes)
      val oauth_signature = DatatypeConverter.printBase64Binary(digest)
      //Base64.encode(digest) ;
      val authorizationProperty = "OAuth " + "realm=\"" + realm + "\", " + "oauth_version=\"" + oauth_version + "\", " + "oauth_timestamp=\"" + oauth_timestamp + "\", " + "oauth_nonce=\"" + oauth_nonce + "\", " + "oauth_consumer_key=\"" + oauth_consumer_key + "\", " + "oauth_token=\"" + oauth_token + "\", " + "oauth_signature_method=\"" + oauth_signature_method + "\", " + "oauth_signature=\"" + oauth_signature + "\""
      val connection = new URL(requestURL).openConnection.asInstanceOf[HttpURLConnection]
      connection.addRequestProperty("Authorization", authorizationProperty)
      connection.connect()
      // from here standard actions...
      // read response code... read input stream.... close connection...
      _lastCode = connection.getResponseCode
      _debug("Response Code is " + _lastCode)
      if (200 == _lastCode || 401 == _lastCode || 404 == _lastCode) {
        val rd = new BufferedReader(new InputStreamReader(if (_lastCode == 200) connection.getInputStream
        else connection.getErrorStream))
        val sb = new StringBuffer
        var line: String = null
        var abort = false
        while (!abort) {
          line = rd.readLine
          if (line != null) {
            sb.append(line)
          } else {
            abort = true
          }
        }
        rd.close()
        _lastContent = sb.toString
        _debug("Response Content is \n" + _lastContent)
      }
      return _lastCode == 200
    } catch {
      case e: Exception =>
        _debug("(!) Error while requesting " + requestURL)
        _lastError = e
    }
    false
  }

  /**
    * Get response code from last request.
    */
  def responseCode: Int = _lastCode

  /**
    * Get response content from last request.
    */
  def responseContent: String = _lastContent
}
