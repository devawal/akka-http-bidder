package bidder

import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import bidder.GuiterDB.{CreateGuiter, FindAllGuiters, GuiterCreated}
import spray.json._

import scala.concurrent.Future

case class Guiter(make: String, model: String)

object GuiterDB {
  case class CreateGuiter(guiter: Guiter)
  case class GuiterCreated(id: Int)
  case class FindGuiter(id: Int)
  case object FindAllGuiters
}

class GuiterDB extends Actor with ActorLogging {
  import GuiterDB._
  var guiters: Map[Int, Guiter] = Map()
  var currentGuiterId: Int = 0

  override def receive: Receive = {
    case FindAllGuiters =>
      log.info("Searching all guiters")
      sender() ! guiters.values.toList

    case FindGuiter(id) =>
      log.info(s"Find Guiter by ID $id")
      sender() ! guiters.get(id)

    case CreateGuiter(guiter) =>
      log.info(s"Adding guiter $guiter with id: $currentGuiterId")
      sender() ! GuiterCreated(currentGuiterId)
      currentGuiterId += 1
  }
}

trait GuiterStoreJsonProtocol extends DefaultJsonProtocol {

  implicit val guiterFormat = jsonFormat2(Guiter)

}

object GuiterApp extends App with GuiterStoreJsonProtocol {
  implicit val system = ActorSystem("LowLevelRest")
  implicit val meterializer = ActorMaterializer()

  import system.dispatcher

  // JSON ->marshalling
  val simpleGuiter = Guiter("Finder", "Stratocast")
  println(simpleGuiter.toJson.prettyPrint)

  val simpleGuiterJsonString =
    """
      |{
      |  "make": "Finder",
      |  "model": "Stratocast"
      |}
    """.stripMargin

  println(simpleGuiterJsonString.parseJson.convertTo[Guiter])

  // Setup
  val guiterDb = system.actorOf(Props[GuiterDB], "GuiterDB")
  val guiterList = List(
    Guiter("Finder", "Stratocast"),
    Guiter("Gipson", "Les paul"),
    Guiter("Martan", "LX1")
  )

  guiterList.foreach{ guiter =>
    guiterDb ! CreateGuiter(guiter)
  }

  // Server code
  implicit val defaultTimeout = Timeout(2 seconds)
  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/bid-request"), _, _, _) =>
      val guitersFuture: Future[List[Guiter]] = (guiterDb ? FindAllGuiters).mapTo[List[Guiter]]
      guitersFuture.map { guiters =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            guiters.toJson.prettyPrint
          )
        )
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/bid-request"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap{ strictEntity =>
        val guiterJsonString = strictEntity.data.utf8String
        val guiter = guiterJsonString.parseJson.convertTo[Guiter]
        val guiterCreatedFuture: Future[GuiterCreated] = (guiterDb ? CreateGuiter(guiter)).mapTo[GuiterCreated]
        guiterCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
        Future {
          HttpResponse(status = StatusCodes.NotFound)
        }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)
}
