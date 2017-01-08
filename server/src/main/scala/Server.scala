import java.util.concurrent.CountDownLatch
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import scala.concurrent.Await
import scala.concurrent.duration._

object Server extends App {
  val logger = Logger(Server.getClass)
  val conf = ConfigFactory.load().withFallback(ConfigFactory.load("default.conf"))

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val shutdownLatch = new CountDownLatch(1)

  val devRoute = pathSingleSlash {
    parameter("fast") { _ =>
      getFromFile("./src/main/resources/static/index-fastopt.html")
    } ~
      getFromFile("./src/main/resources/static/index-fullopt.html")
  } ~
    encodeResponse {
      getFromDirectory("./src/main/resources/static") ~
        pathPrefix("js") {
          getFromDirectory("../js/target/scala-2.11")
        }
    }

  val productionRoute = pathSingleSlash {
    parameter("fast") { _ =>
      getFromResource("static/index-fastopt.html")
    } ~
      getFromResource("static/index-fullopt.html")
  } ~
    encodeResponse {
      getFromResourceDirectory("static")
    }

  val host = conf.getString("server.host")
  val port = conf.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(devRoute ~ productionRoute, host, port)
  logger.info(s"Server online as http://${host}:${port}")

  Runtime.getRuntime().addShutdownHook(new Thread() {
                                         override def run() = {
                                           val f = bindingFuture.flatMap(_.unbind()) andThen {
                                             case _ => actorSystem.terminate()
                                           }
                                           Await.ready(f, 1 minute)
                                           logger.info("Goodbye!")
                                           shutdownLatch.countDown()
                                         }
                                       })

  shutdownLatch.await()
}
