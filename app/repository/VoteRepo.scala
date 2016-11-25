package repository

import java.net.URL
import javax.inject.{ Inject, Singleton }

import akka.agent.Agent
import com.google.inject.ImplementedBy
import models.DropboxResponses.{ SaveStatus, Saved }
import play.api.Configuration
import play.api.libs.ws.WSClient
import utils.DropboxUtil

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

@ImplementedBy(classOf[VoteRepoSimple])
trait VoteRepo {
  /**
   * @return report in the form of a map
   */
  def getReport: Future[Map[String, Long]]

  /**
   * @param imageName name of the image
   * @return vote count after the increment. None if image does not exist
   */
  def vote(imageName: String): Future[Option[Long]]

  /**
   * @param imageUrl URL of the image
   * @return status of current save operation
   */
  def addImage(imageUrl: URL): Future[SaveStatus]
}

@Singleton
final class VoteRepoSimple @Inject() (val ws: WSClient,
                                      val conf: Configuration,
                                      implicit val ec: ExecutionContext) extends VoteRepo with DropboxUtil {
  private val agent = Agent(Map.empty[String, Long]) // Using Akka agent for thread safety

  def getReport: Future[Map[String, Long]] = {
    def addDiff(map: Map[String, Long], diff: Set[String]): Map[String, Long] =
      diff.foldLeft(map) { (newMap, imageName) => newMap + (imageName -> 0) } // add images that are not currently in the map
    for {
      allFiles <- listFiles
      curMap <- agent.future
      newMap <- agent.alter(map => addDiff(map, allFiles diff curMap.keySet))
    } yield newMap
  }

  def vote(imageName: String): Future[Option[Long]] = {
    def alterMap(map: Map[String, Long]): Map[String, Long] = map.get(imageName)
      .map(count => map + (imageName -> (count + 1)))
      .getOrElse(map + (imageName -> 1))
    listFiles.map(_.contains(imageName)).flatMap {
      case true => agent.alter(map => alterMap(map)).map(_.get(imageName))
      case false => Future.successful(None)
    }
  }

  def addImage(imageUrl: URL): Future[SaveStatus] = save(imageUrl) andThen {
    case Success(Saved(imageName)) => agent send (_ + (imageName -> 0))
  }
}