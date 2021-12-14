import api.ApiRoute
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.LazyLogging
import components.{ControllerComponent, ServiceComponent, StorageComponent}
import configs.AppConfig
import storage.DoobieDatabase
import org.http4s.blaze.server.BlazeServerBuilder
import scala.concurrent.ExecutionContext.Implicits.global
object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      appConfig           <- Resource.eval(AppConfig.defaultLoadF[IO])
      transactor          <- DoobieDatabase.transactor[IO](appConfig.doobie, global)
      _                   <- Resource.eval(DoobieDatabase.performMigrations(transactor))
      storageComponent    <- Resource.eval(StorageComponent[IO](transactor))
      serviceComponent    <- Resource.eval(ServiceComponent[IO](storageComponent))
      controllerComponent <- Resource.eval(ControllerComponent[IO](serviceComponent))
      serverResource <- BlazeServerBuilder[IO]
                         .bindHttp(appConfig.server.port, appConfig.server.host)
                         .withHttpApp(
                           ApiRoute
                             .getRoutes(
                               controllerComponent.taskController.serverEndpoints ++
                                 controllerComponent.authController.serverEndpoints
                             )
                             .orNotFound
                         )
                         .resource
    } yield serverResource).use(_ => IO(logger.info("The application has started")) >> IO.never).guaranteeCase {
      case Succeeded(_) => IO.delay(logger.info("Fiber successfully completed "))
      case Errored(e)   => IO.delay(logger.info(s"Fiber completed with error: $e"))
      case Canceled()   => IO.delay(logger.info("Fiber got cancelled"))
    }

  }
}
