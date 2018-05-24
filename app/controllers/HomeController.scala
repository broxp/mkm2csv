package controllers

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}

case class ApiCallData(
                        appType: String,
                        appToken: String,
                        appSecret: String,
                        accessToken: String,
                        accessTokenSecret: String
                      )

class HomeController @Inject()(mkmController: MkmController, cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {
  val formApiCallData = Form(mapping(
    "appType" -> default(text, ""),
    "appToken" -> nonEmptyText,
    "appSecret" -> nonEmptyText,
    "accessToken" -> nonEmptyText,
    "accessTokenSecret" -> nonEmptyText
  )(ApiCallData.apply)(ApiCallData.unapply))

  def index = Action { implicit req =>
    Ok(views.html.index("", "", formApiCallData))
  }

  def post() = Action { implicit req =>
    formApiCallData.bindFromRequest()(req) match {
      case Form(_, data, _, Some(value)) =>
        try {
          val res = mkmController.call(value)
          Ok(views.html.index("Ok, see csv below", res, formApiCallData.bind(data)))
        } catch {
          case x: Exception =>
            Ok(views.html.index("Error, see details below", x.getMessage, formApiCallData.bind(data)))
        }
      case Form(_, data, err, _) =>
        Ok(views.html.index("Errors: " + err, "", formApiCallData.bind(data)))
    }


  }
}
