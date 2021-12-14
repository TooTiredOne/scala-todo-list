package api.auth

import api.endpoints.baseEndpoint
import api.errors.{AppError, Unauthorized}
import cats.Applicative
import cats.effect.Sync
import cats.syntax.either._
import sttp.tapir._
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.PartialServerEndpoint

final class AuthorizedEndpointBuilder[F[_]: Applicative](authService: AuthService[F]) {
  def authorizedEndpoint: PartialServerEndpoint[UsernamePassword, AuthSession, Unit, AppError, Unit, Any, F] =
    baseEndpoint
      .securityIn(auth.basic[UsernamePassword]())
      .in("api")
      .serverSecurityLogic { authInfo =>
        authInfo.password match {
          case Some(password) => authService.authorize(authInfo.username, password)
          case None           => Applicative[F].pure(Unauthorized("authorization failed").asLeft[AuthSession])
        }
      }
}
object AuthorizedEndpointBuilder {
  def apply[F[_]: Sync](authService: AuthService[F]): F[AuthorizedEndpointBuilder[F]] =
    Sync[F].delay(new AuthorizedEndpointBuilder[F](authService))
}
