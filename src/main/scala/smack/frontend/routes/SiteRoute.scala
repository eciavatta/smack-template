package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import smack.frontend.server.RestRoute
import smack.models.messages._
import smack.models.structures.Site
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


case class SiteRoute(backendRouter: ActorRef)(implicit val config: Config) extends RestRoute {
  import smack.frontend.routes.SiteRoute._

  override def route: Route =
    pathPrefix("sites") {
      pathEndOrSingleSlash {
        get {
          entity(as[SitesListing]) { sitesListing =>
            handle(ListSitesRequest(sitesListing.userId), (res: ListSitesResponse) => res.sites)
          }
        } ~
        post {
          entity(as[SiteCreating]) { siteCreating =>
            handle(CreateSiteRequest(siteCreating.userId, siteCreating.domain), (res: CreateSiteResponse) => res.site)
          }
        }
      }
    } ~
    pathPrefix("sites" / Segment) { id =>
      pathEndOrSingleSlash {
        get {
          handle(FindSiteRequest(id), (res: FindSiteResponse) => res.site)
        } ~
        put {
          entity(as[SiteUpdating]) { siteUpdating =>
            handle(UpdateSiteRequest(id, siteUpdating.domain), (res: UpdateSiteResponse) => res.site)
          }
        } ~
        delete {
          entity(as[SiteDeleting]) { siteDeleting =>
            handle(DeleteSiteRequest(id, siteDeleting.userId), (_: DeleteSiteResponse) => Option.empty[Site])
          }
        }
      }
    }
}

object SiteRoute extends DefaultJsonProtocol {

  case class SitesListing(userId: String)
  case class SiteCreating(userId: String, domain: String)
  case class SiteUpdating(domain: String)
  case class SiteDeleting(userId: String)

  implicit val sitesListingFormat: RootJsonFormat[SitesListing] = jsonFormat1(SitesListing.apply)
  implicit val siteCreatingFormat: RootJsonFormat[SiteCreating] = jsonFormat2(SiteCreating.apply)
  implicit val siteUpdatingFormat: RootJsonFormat[SiteUpdating] = jsonFormat1(SiteUpdating.apply)
  implicit val siteDeletingFormat: RootJsonFormat[SiteDeleting] = jsonFormat1(SiteDeleting.apply)

}
