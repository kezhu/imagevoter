package models

import java.net.URL
import java.nio.file.Paths

import org.apache.commons.io.FilenameUtils
import play.api.libs.json._
import play.api.libs.functional.syntax._

object SaveImageRequest {
  implicit val saveImageRequestWrites: Writes[SaveImageRequest] = (
    (JsPath \ "path").write[String] and
    (JsPath \ "url").write[String]
  )(unlift(SaveImageRequest.unapply))

  def apply(imageUrl: URL, folderPath: String): SaveImageRequest = {
    val imageName = FilenameUtils.getName(imageUrl.getPath)
    val path = Paths.get(folderPath, imageName).toString
    new SaveImageRequest(path, imageUrl.toString)
  }

  def unapply(arg: SaveImageRequest): Option[(String, String)] = Some((arg.path, arg.url))
}

class SaveImageRequest(val path: String, val url: String)