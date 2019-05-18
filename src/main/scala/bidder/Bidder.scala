package bidder

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.javadsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.ActorMaterializer
import akka.util.Timeout
import bidder.bidderDataMap.getBidderData

import scala.concurrent.duration._
import akka.pattern.ask
import spray.json._

import scala.concurrent.Future

case class requestData(name: String, id: Int)

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
// This will return as HTTP JSON response
case class BidResponse(id: String, bidRequestId: String, price: Double, adid: Option[String], banner: Option[Banner])

object bidderDataMap {
  case class getBidderData(bidReq: requestData)
}

class bidderDataMap extends Actor with ActorLogging {
  import bidderDataMap._

  val bidder = Map[String, requestData]()

  override def receive: Receive = {
    case getBidderData(bidReq) =>
      sender() ! bidder.values.toList
  }
}

trait ServiceJsonProtoocol extends DefaultJsonProtocol {
  implicit val customerProtocol = jsonFormat2(requestData)
}

object Bidder extends App with ServiceJsonProtoocol{
  implicit val system = ActorSystem("bid_request")
  implicit val meterializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  val dataBiddermap = system.actorOf(Props[bidderDataMap], "bidderDataMap")

  // Data feed
  val cityList = List("Dhaka", "Rajshahi", "Chattagram")
  val siteList = List("prothomalo.com", "jobs.bdjobs.com", "cricinfo.com", "cricbuzz.com")
  val simpleTime = TimeRange(1526320800, 1527703200)
  val simpleTargeting = Targeting(cityList, siteList)
  val simpleBanner1 = Banner(112, "http://dummyimage.com/300x250", 300, 250)
  val simpleBanner2 = Banner(113, "http://dummyimage.com/300x100", 300, 100)

  val campaignData = Campaign(223, 12, "Bangladesh", Set(simpleTime), simpleTargeting, List(simpleBanner1, simpleBanner2), 1.23)

  val uuid = java.util.UUID.randomUUID.toString

  val responseData = BidResponse(uuid, "3dfgfg", campaignData.bid, Option("r"), Option(simpleBanner1))

  // Define common HTTP entity
  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  implicit val timeOut = Timeout(2 seconds)


  // Server code
  val httpServerRoute =
     post {
       path("bid-request")
       entity(implicitly[FromRequestUnmarshaller[requestData]]) { request =>
         complete((dataBiddermap ? getBidderData(request)).map(
           _ => StatusCodes.OK
         ))
       }
     }

  Http().bindAndHandle(httpServerRoute, "localhost", 8080)
}
