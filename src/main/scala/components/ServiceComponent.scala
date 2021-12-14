package components

import api.auth.AuthService
import api.task.TaskService
import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._

case class ServiceComponent[F[_]](
    authService: AuthService[F],
    taskService: TaskService[F]
)
object ServiceComponent {
  def apply[F[_]: Sync](storageComponent: StorageComponent[F]): F[ServiceComponent[F]] =
    for {
      authService <- AuthService[F](storageComponent.userStorage)
      taskService <- TaskService[F](storageComponent.taskStorage)
    } yield ServiceComponent[F](authService, taskService)
}
