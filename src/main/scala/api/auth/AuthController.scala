package api.auth

import api.endpoints.baseEndpoint
import api.errors.AppError
import cats.effect.kernel.Sync
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

class AuthController[F[_]](authService: AuthService[F]) {
  lazy val registerEndpoint: Endpoint[Unit, AuthInfoRequest, AppError, Unit, Any] =
    baseEndpoint.post
      .in("api" / "register")
      .in(jsonBody[AuthInfoRequest])
      .description("Register new user")

  def serverEndpoints: List[ServerEndpoint[Any, F]] = List(
    registerEndpoint.serverLogic { authInfoReq =>
      authService.register(authInfoReq.username, authInfoReq.password)
    }
  )
}

object AuthController {
  def apply[F[_]: Sync](authService: AuthService[F]): F[AuthController[F]] =
    Sync[F].delay(new AuthController[F](authService))
}
