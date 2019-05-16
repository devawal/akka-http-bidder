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
case class Targeting(cities: List[String], targetedSiteIds: Long)
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

/**
  * Based on the code found: https://groups.google.com/forum/#!topic/spray-user/RkIwRIXzDDc
  */
class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue = JsString(obj.toString)

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case somethingElse => throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
    }
  }
}

trait BidderJsonProtocol extends DefaultJsonProtocol {
  implicit val bidRequestFormat = new EnumJsonConverter(BidRequest)

}

object Bidder extends App with BidderJsonProtocol {
  implicit val system = ActorSystem("Bidder API")
  implicit val meterializer = ActorMaterializer()


  // Server code
  implicit val defaultTimeout = Timeout(2 seconds)

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.POST, Uri.Path("/bid-request"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)
}
