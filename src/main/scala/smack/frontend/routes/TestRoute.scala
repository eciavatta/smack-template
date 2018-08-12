package smack.frontend.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import smack.common.mashallers.Marshalling
import smack.frontend.server.RestRoute
import smack.models.messages._

class TestRoute(protected val backendRouter: ActorRef)(protected implicit val requestTimeout: Timeout) extends RestRoute with Marshalling {

  override def route: Route =
    pathPrefix("test" / Segment) { s =>
      pathEndOrSingleSlash {
        get {
          handle(TestRequest(s), (t: TestResponse) => t.value)
        }
      }
    }
}
