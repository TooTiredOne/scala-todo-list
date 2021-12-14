package api

import cats.effect.kernel.{Async, Sync}
import com.typesafe.scalalogging.LazyLogging
import org.http4s.HttpRoutes
import sttp.monad.MonadError
import sttp.tapir.integ.cats.CatsMonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object ApiRoute extends LazyLogging {
  def getRoutes[F[_]: Async](endpoints: List[ServerEndpoint[Any, F]]): HttpRoutes[F] = {
    implicit val monadError: MonadError[F] = new CatsMonadError[F]
    val customServerOptions: Http4sServerOptions[F, F] = Http4sServerOptions
      .customInterceptors[F, F]
      .serverLog(
        DefaultServerLog[F](
          doLogWhenHandled = (msg, exOpt) =>
            exOpt match {
              case None     => Sync[F].delay(logger.info(msg))
              case Some(ex) => Sync[F].delay(logger.info(msg, ex))
            },
          doLogAllDecodeFailures = (msg, exOpt) =>
            exOpt match {
              case None     => Sync[F].delay(logger.debug(msg))
              case Some(ex) => Sync[F].delay(logger.debug(msg, ex))
            },
          doLogExceptions = (msg, ex) => Sync[F].delay(logger.error(msg, ex))
        )
      )
      .options
    Http4sServerInterpreter[F](customServerOptions).toRoutes(endpoints ++ swaggerRoutes(endpoints))
  }

  private def swaggerRoutes[F[_]](endpoints: List[ServerEndpoint[Any, F]]): List[ServerEndpoint[Any, F]] = {
    SwaggerInterpreter().fromServerEndpoints[F](endpoints, "Todo list", "0.1")
  }

}
