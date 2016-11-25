package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object DropboxResponses {
  // JSON deserializer of JSON responses from Dropbox
  implicit val saveUrlResultReads: Reads[InProgress] = (JsPath \ "async_job_id").read[String].map(InProgress)
  implicit val fileMetadataReads: Reads[Saved] = (JsPath \ "name").read[String].map(Saved)
  implicit val errorResultReads: Reads[Failed] = (JsPath \ "error_summary").read[String].map(Failed)
  implicit val listFolderResultReads: Reads[Set[String]] = (JsPath \ "entries").read[Seq[Saved]].map(_.map(_.name).toSet)

  sealed trait SaveStatus
  case class Saved(name: String) extends SaveStatus
  case class InProgress(jobId: String) extends SaveStatus
  case class Failed(errorSummary: String) extends Exception with SaveStatus

  case class UnknownResponseException(json: JsValue) extends Exception
}
