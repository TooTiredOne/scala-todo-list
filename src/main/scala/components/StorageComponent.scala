package components

import cats.effect.{MonadCancel, Sync}
import doobie.util.transactor.Transactor
import storage.auth.UserStorage
import storage.task.TaskStorage
import cats.syntax.flatMap._
import cats.syntax.functor._

case class StorageComponent[F[_]](
    taskStorage: TaskStorage[F],
    userStorage: UserStorage[F]
)

object StorageComponent {
  def apply[F[_]: Sync](
      transactor: Transactor[F]
  )(implicit monadCancel: MonadCancel[F, Throwable]): F[StorageComponent[F]] =
    for {
      taskStorage <- TaskStorage[F](transactor)
      userStorage <- UserStorage[F](transactor)
    } yield StorageComponent(taskStorage, userStorage)
}
