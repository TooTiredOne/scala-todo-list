package api.auth

import api.errors.{AppError, BadRequest, InternalServerError, Unauthorized}
import cats.Applicative
import cats.effect.kernel.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import storage.auth.UserStorage
import storage.auth.error.UserDbError
import tsec.common.{VerificationFailed, Verified}
import tsec.passwordhashers._
import tsec.passwordhashers.jca._

trait AuthService[F[_]] {
  def register(username: String, password: String): F[Either[AppError, Unit]]
  def authorize(username: String, password: String): F[Either[AppError, AuthSession]]
}

object AuthService {
  def apply[F[_]: Sync](storage: UserStorage[F]): F[AuthServiceImpl[F]] =
    Sync[F].delay(new AuthServiceImpl[F](storage))
}

class AuthServiceImpl[F[_]: Sync](storage: UserStorage[F]) extends AuthService[F] {
  override def register(username: String, password: String): F[Either[AppError, Unit]] = {
    SCrypt.hashpw[F](password).flatMap { passwordHash =>
      storage.insertUser(username, passwordHash).map {
        case Right(_) => ().asRight[AppError]
        case Left(ex) =>
          ex match {
            case UserDbError.UniqueValidationError(_) => BadRequest("user with such name already exists").asLeft[Unit]
            case UserDbError.GeneralError(cause)      => InternalServerError(cause).asLeft[Unit]
          }
      }
    }
  }

  override def authorize(username: String, password: String): F[Either[AppError, AuthSession]] =
    storage.getUser(username).flatMap {
      case Right(optUserDto) =>
        optUserDto match {
          case Some(userDto) =>
            SCrypt.checkpw[F](password, PasswordHash(userDto.password_hash)).map {
              case Verified           => AuthSession(userDto.username, userDto.id).asRight[AppError]
              case VerificationFailed => Unauthorized("authentication failed").asLeft[AuthSession]
            }
          case None => Applicative[F].pure(Unauthorized("authentication failed").asLeft[AuthSession])
        }
      case Left(ex) => Applicative[F].pure(InternalServerError(ex.cause).asLeft[AuthSession])
    }
}
