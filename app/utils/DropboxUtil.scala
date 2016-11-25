package utils

import java.net.URL

import models.SaveImageRequest
import models.DropboxResponses._
import play.api.Configuration
import play.api.libs.json.{ JsError, JsSuccess, Json }
import play.api.libs.ws.WSClient

import scala.concurrent.{ ExecutionContext, Future }

trait DropboxUtil {
  val ws: WSClient
  val conf: Configuration
  implicit val ec: ExecutionContext

  val token: String = conf.getString("dropbox.token").get
  val folderPath: String = conf.getString("dropbox.folder").get

  final def save(url: URL): Future[SaveStatus] = ws.url("https://api.dropboxapi.com/2/files/save_url")
    .withHeaders("Authorization" -> s"Bearer $token", "Content-Type" -> "application/json")
    .post(Json.toJson(SaveImageRequest(url, folderPath)))
    .map { response =>
      val json = response.json
      json.validate[Saved].orElse(json.validate[InProgress]).orElse(json.validate[Failed]) match {
        case JsSuccess(ex: Failed, _) => throw ex
        case JsSuccess(status: SaveStatus, _) => status
        case _: JsError => throw UnknownResponseException(json)
      }
    }

  final def listFiles: Future[Set[String]] = ws.url("https://api.dropboxapi.com/2/files/list_folder")
    .withHeaders("Authorization" -> s"Bearer $token", "Content-Type" -> "application/json")
    .post(Json.obj("path" -> folderPath))
    .map { response =>
      val json = response.json
      json.validate[Set[String]] match {
        case JsSuccess(names: Set[String], _) => names
        case _: JsError => json.validate[Failed] match {
          case JsSuccess(ex: Failed, _) => throw ex
          case _: JsError => throw UnknownResponseException(json)
        }
      }
    }

}
