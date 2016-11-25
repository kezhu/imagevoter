package controllers

import javax.inject.Inject

import models.{ BurnerRequest, InvalidTypeException }
import models.DropboxResponses.{ Failed, InProgress, Saved, UnknownResponseException }
import play.api.libs.json.{ JsError, Json, Reads }
import play.api.mvc._
import repository.VoteRepo

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class Vote @Inject() (voteReportRepo: VoteRepo, implicit val ec: ExecutionContext) extends Controller {

  def validateJson[A: Reads]: BodyParser[A] = BodyParsers.parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  // Print messages to std out for now
  val errorHandler: PartialFunction[Throwable, Result] = {
    case Failed(errorSummary) => BadRequest(errorSummary)
    case InvalidTypeException(msg) => BadRequest(msg)
    case UnknownResponseException(json) =>
      println(s"Received unknown response from Dropbox: $json")
      InternalServerError
    case NonFatal(ex) =>
      println(s"Unexpected error happened: $ex")
      InternalServerError
  }

  def handleEvent(): Action[BurnerRequest] = Action.async(validateJson[BurnerRequest]) { req =>
    req.body.getImageUrl.flatMap {
      case Some(url) => voteReportRepo.addImage(url).map {
        case Saved(name) => Created(name) // image saved to Dropbox
        case InProgress(_) => Accepted // image accepted by Dropbox but not fully uploaded yet
        case ex: Failed => throw ex
      }
      case None => req.body.getImageName.flatMap {
        case Some(name) => voteReportRepo.vote(name).map(_ => NoContent) // return 204 regardless if the image exists
        case None => Future.successful(NoContent) // neither a valid image nor vote, return 204
      }
    }.recover(errorHandler)
  }

  def getReport: Action[AnyContent] = Action.async {
    voteReportRepo.getReport.map(report => Ok(Json.toJson(report))).recover(errorHandler)
  }
}
