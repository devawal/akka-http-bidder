package bidder

import akka.actor.{ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import spray.json._

import scala.concurrent.Future


//Campaign protocol:
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


object Bidder extends App {
  implicit val system = ActorSystem("bid_request")
  implicit val meterializer = ActorMaterializer()
  import akka.http.scaladsl.server.Directive._

  // Data feed
  val cityList = List("Dhaka", "Rajshahi", "Chattagram")
  val siteList = List("prothomalo.com", "jobs.bdjobs.com", "cricinfo.com", "cricbuzz.com")
  val simpleTime = TimeRange(1526320800, 1527703200)
  val simpleTargeting = Targeting(cityList, siteList)
  val simpleBanner1 = Banner(112, "http://dummyimage.com/300x250", 300, 250)
  val simpleBanner2 = Banner(113, "http://dummyimage.com/300x100", 300, 100)

  val campaignData = Campaign(223, 12, "Bangladesh", Set(simpleTime), simpleTargeting, List(simpleBanner1, simpleBanner2), 1.23)

  println(campaignData.country)



  // Server code
  
}
