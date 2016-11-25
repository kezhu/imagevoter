package models

import java.net.URL

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.Future

object RequestType {
  def apply(str: String): RequestType = str match {
    case "inboundText" => InboundText
    case "inboundMedia" => InboundMedia
    case "voiceMail" => VoiceMail
    case _ => throw InvalidTypeException(s"$str is not a valid type")
  }
}

sealed trait RequestType
case object InboundText extends RequestType
case object InboundMedia extends RequestType
case object VoiceMail extends RequestType

case class InvalidTypeException(msg: String) extends Exception

object BurnerRequest {
  /**
   * Serializer that converts request JSON into a BurnerRequest object
   */
  implicit val burnerRequestReads: Reads[BurnerRequest] = (
    (JsPath \ "type").read[String] and
    (JsPath \ "payload").read[String] and
    (JsPath \ "fromNumber").read[String] and
    (JsPath \ "toNumber").read[String] and
    (JsPath \ "userId").read[String] and
    (JsPath \ "burnerId").read[String]
  )(BurnerRequest.apply _)

  def apply(requestType: String,
            payload: String,
            fromNumber: String,
            toNumber: String,
            userId: String,
            burnerId: String): BurnerRequest =
    new BurnerRequest(RequestType(requestType), payload, fromNumber, toNumber, userId, burnerId)

  def unapply(arg: BurnerRequest): Option[(RequestType, String, String, String, String, String)] =
    Some((arg.requestType, arg.payload, arg.fromNumber, arg.toNumber, arg.userId, arg.burnerId))
}

class BurnerRequest private (
    val requestType: RequestType,
    val payload: String,
    val fromNumber: String,
    val toNumber: String,
    val userId: String,
    val burnerId: String)
{
  /**
   * @return url of the image if the request's payload contains an image url otherwise None
   */
  def getImageUrl: Future[Option[URL]] = {
    // determine if the url contains a image. Always return current url for now... Could use Tika to determine its content
    def filterImage(url: URL): Future[Option[URL]] = Future.successful(Some(url))
    requestType match {
      case InboundMedia => filterImage(new URL(payload))
      case _ => Future.successful(None)
    }
  }

  /**
   * @return name of the image if the request's payload contains text otherwise None
   */
  def getImageName: Future[Option[String]] = requestType match {
    case InboundText => Future.successful(Some(payload))
    case _ => Future.successful(None)
  }
}
