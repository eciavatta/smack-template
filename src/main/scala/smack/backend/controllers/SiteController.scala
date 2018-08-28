package smack.backend.controllers

import akka.actor.{Actor, ActorLogging, Props}
import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import com.fasterxml.uuid.Generators
import smack.cassandra.CassandraDatabase.CassandraStatement
import smack.commons.traits.CassandraController
import smack.commons.traits.Controller.NotFoundException
import smack.commons.utils.Converters
import smack.models.messages._
import smack.models.structures.{Date, Site}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class SiteController extends Actor with CassandraController with ActorLogging {

  override def receive: Receive = {
    case ListSitesRequest(userId) =>
      val _sender = sender()
      listSites(userId).onComplete {
        case Success(sites) => _sender ! ListSitesResponse(success(), sites)
        case Failure(ex) => _sender ! ListSitesResponse(responseRecovery(ex))
      }
    case FindSiteRequest(id) =>
      val _sender = sender()
      findSite(id).onComplete {
        case Success(site) => _sender ! FindSiteResponse(success(), Some(site))
        case Failure(ex) => _sender ! FindSiteResponse(responseRecovery(ex))
      }
    case CreateSiteRequest(userId, domain) =>
      val _sender = sender()
      createSite(userId, domain).onComplete {
        case Success(site) => _sender ! CreateSiteResponse(success(), Some(site))
        case Failure(ex) => _sender ! CreateSiteResponse(responseRecovery(ex))
      }
    case UpdateSiteRequest(id, domain) =>
      val _sender = sender()
      updateSite(id, domain).onComplete {
        case Success(site) => _sender ! UpdateSiteResponse(success(), Some(site))
        case Failure(ex) => _sender ! UpdateSiteResponse(responseRecovery(ex))
      }
    case DeleteSiteRequest(id, userId) =>
      val _sender = sender()
      deleteSite(id, userId).onComplete {
        case Success(_) => _sender ! DeleteSiteResponse(success())
        case Failure(ex) => _sender ! DeleteSiteResponse(responseRecovery(ex))
      }
    }

  private def listSites(userId: String): Future[List[Site]] = Future
    .fromTry(convertToUUID(userId))
    .map { uuid =>
      CassandraStatement(new SimpleStatement("SELECT site_id, toTimestamp(site_id) as created_date FROM sites_by_users WHERE user_id = ?;", uuid))
    }
    .flatMap(executeQuery)
    .map { resultSet =>
      resultSet.all().asScala.map { row =>
        Site(id = row.getUUID("site_id").toString,
          createdDate = Some(Date(row.getTimestamp("created_date").getTime))
        )
      }.toList
    }

  private def findSite(id: String): Future[Site] = Future
    .fromTry(convertToUUID(id))
    .map { uuid =>
      CassandraStatement(new SimpleStatement("SELECT id, domain, tracking_id, toTimestamp(id) as created_date FROM sites_by_id WHERE id = ?;", uuid))
    }
    .flatMap(executeQuery)
    .map { resultSet =>
      resultSet.all().asScala.headOption.fold(throw NotFoundException) { row =>
        Site(id = row.getUUID("id").toString,
          domain = row.getString("domain"),
          trackingId = row.getUUID("tracking_id").toString,
          createdDate = Some(Date(row.getTimestamp("created_date").getTime))
        )
      }
    }

  private def createSite(userId: String, domain: String): Future[Site] = Future
    .fromTry(convertToUUID(userId))
    .flatMap { userUUID =>
      val id = Generators.timeBasedGenerator().generate()
      val trackingId = Generators.randomBasedGenerator().generate()
      val queries = Seq(
        new SimpleStatement("INSERT INTO sites_by_id (id, domain, user_id, tracking_id) VALUES (?, ?, ?, ?);", id, domain, userUUID, trackingId),
        new SimpleStatement("INSERT INTO sites_by_tracking_id (tracking_id, site_id) VALUES (?, ?);", trackingId, id),
        new SimpleStatement("INSERT INTO sites_by_users (user_id, site_id) VALUES (?, ?);", userUUID, id)
      )
      executeQuery(CassandraStatement(new BatchStatement().addAll(queries.asJava)))
        .map(_ => Site(id.toString, domain, trackingId.toString, Some(Date(Converters.getTimeFromUUID(id)))))
    }

  private def updateSite(id: String, domain: String): Future[Site] = findSite(id)
    .flatMap { site =>
      executeQuery(
        CassandraStatement(new SimpleStatement("UPDATE sites_by_id SET domain = ? WHERE id = ?;", domain, convertToUUID(id).get))
      ).map (_ => site.copy(domain = domain))
    }

  private def deleteSite(id: String, userId: String): Future[Unit] = findSite(id)
    .map { site =>
      val id = convertToUUID(site.id).get
      val queries = Seq(
        new SimpleStatement("DELETE FROM sites_by_id WHERE id = ?;", id),
        new SimpleStatement("DELETE FROM sites_by_tracking_id WHERE tracking_id = ?;", convertToUUID(site.trackingId).get),
        new SimpleStatement("DELETE FROM sites_by_users WHERE user_id = ? AND site_id = ?;", convertToUUID(userId).get, id)
      )
      executeQuery(CassandraStatement(new BatchStatement().addAll(queries.asJava)))
    }

}

object SiteController {
  def props: Props = Props(new SiteController)
  def name: String = "siteController"
}
