package storage.auth

import cats.effect.{MonadCancel, Sync}
import cats.syntax.either._
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import doobie.postgres.sqlstate
import storage.auth.error.UserDbError

trait UserStorage[F[_]] {
  def getUser(username: String): F[Either[UserDbError, Option[UserDto]]]
  def insertUser(username: String, password_hash: String): F[Either[UserDbError, UserDto]]
}
object UserStorage {
  def apply[F[_]: Sync](
      transactor: Transactor[F]
  )(implicit monadCancel: MonadCancel[F, Throwable]): F[UserStorageImpl[F]] =
    Sync[F].delay(new UserStorageImpl[F](transactor))
}

final class UserStorageImpl[F[_]](transactor: Transactor[F])(implicit monadCancel: MonadCancel[F, Throwable])
    extends UserStorage[F] {
  override def getUser(username: String): F[Either[UserDbError, Option[UserDto]]] =
    queries
      .getUser(username)
      .option
      .transact(transactor)
      .attemptSql
      .map(_.leftMap(ex => UserDbError.GeneralError(ex.getMessage)))

  override def insertUser(username: String, password_hash: String): F[Either[UserDbError, UserDto]] =
    queries
      .insertUser(username, password_hash)
      .withUniqueGeneratedKeys[UserDto]("id", "username", "password_hash")
      .transact(transactor)
      .attemptSqlState
      .map(_.leftMap {
        case cause @ sqlstate.class23.UNIQUE_VIOLATION => UserDbError.UniqueValidationError("username must be unique")
        case sqlState                                  => UserDbError.GeneralError(sqlState.value)
      })
}

object queries {
  def getUser(username: String): doobie.Query0[UserDto] =
    sql"select id, username, password_hash from users where username = $username"
      .query[UserDto]

  def insertUser(username: String, password_hash: String): doobie.Update0 =
    sql"insert into users (username, password_hash) values ($username, $password_hash)".update
}
