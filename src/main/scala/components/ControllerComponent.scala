package components

import api.auth.{AuthController, AuthorizedEndpointBuilder}
import api.task.TaskController
import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.flatMap._

case class ControllerComponent[F[_]](
    authorizedEndpointBuilder: AuthorizedEndpointBuilder[F],
    taskController: TaskController[F],
    authController: AuthController[F]
)

object ControllerComponent {
  def apply[F[_]: Async](serviceComponent: ServiceComponent[F]): F[ControllerComponent[F]] =
    for {
      authorizedEndpointBuilder <- AuthorizedEndpointBuilder[F](serviceComponent.authService)
      taskController            <- TaskController[F](authorizedEndpointBuilder, serviceComponent.taskService)
      authController            <- AuthController[F](serviceComponent.authService)
    } yield ControllerComponent(authorizedEndpointBuilder, taskController, authController)
}
