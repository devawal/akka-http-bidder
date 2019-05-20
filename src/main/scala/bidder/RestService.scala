package bidder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import spray.json.{DefaultJsonProtocol, JsObject}

case class Template(id: String, w: Int, h: Int, site: String)

// Campaign protocol:
case class TimeRange(timeStart: Long, timeEnd: Long)
case class Targeting(cities: List[String], targetedSiteIds: List[String])
case class Banner(id: Int, src: String, width: Int, height: Int)

case class Campaign(id: Int, userId: Int, country: String, runningTimes: Set[TimeRange], targeting: Targeting, banners: List[Banner], bid: Double)

// BidRequest protocol:
case class Geo(country: Option[String], city: Option[String], lat: Option[Double], lon: Option[Double])
case class User(id: String, geo: Option[Geo])
case class Device(id: String, geo: Option[Geo])
case class Site(id: Int, domain: String)
case class Impression(id: String, wmin: Option[Int], wmax: Option[Int], w: Option[Int], hmin: Option[Int], hmax: Option[Int], h: Option[Int], bidFloor: Option[Double])

// This class will take POST JSON data and process for internal matching
case class BidRequest(id: String, imp: Option[List[Impression]], site: Site, user: Option[User], device: Option[Device])

// BidResponse protocol:
case class BidResponse(id: String, bidRequestId: String, price: Double, adid: Option[Int], banner: Option[Banner])

object RestService extends App with Directives with SprayJsonSupport with DefaultJsonProtocol{

  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  implicit val templateFormat = jsonFormat4(Template)

  // Data feed
  val cityList = List("Dhaka", "Rajshahi", "Chattagram")
  val siteList = List("prothomalo.com", "www.bdjobs.com", "cricinfo.com", "cricbuzz.com")
  val simpleTime = TimeRange(1526320800, 1527703200)
  val simpleTargeting = Targeting(cityList, siteList)
  implicit val simpleBanner1 = Banner(112, "http://dummyimage.com/300x250", 300, 250)
  implicit val simpleBanner2 = Banner(113, "http://dummyimage.com/300x100", 300, 100)

  val campaignData = Campaign(223, 12, "Bangladesh", Set(simpleTime), simpleTargeting, List(simpleBanner1, simpleBanner2), 1.23)

  val uuid = java.util.UUID.randomUUID.toString

  import spray.json._

  def route: Route = {
    post {
      entity(as[Template]) { // unmarshaller applied
        template: Template =>
          complete {
            if (siteList contains(template.site)) {
              val responseData = BidResponse(uuid, template.id, campaignData.bid, Option(campaignData.id), Option(simpleBanner1))

              implicit val bannerFormat = jsonFormat4(Banner)
              implicit val bidResponseFormat = jsonFormat5(BidResponse)

              // Send response
              responseData.toJson
            } else {
              StatusCodes.NoContent
            }

            //println(template.site)
            //template // marshaller applied
          }
      }
    }
  }

  Http().bindAndHandle(route, "127.0.0.1", 8080)

  //readLine()
  //sys.shutdown()
}