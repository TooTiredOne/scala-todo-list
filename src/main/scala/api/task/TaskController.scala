package api.task

import api.auth.{AuthSession, AuthorizedEndpointBuilder}
import api.errors.AppError
import cats.effect.kernel.{Async, Sync}
import com.typesafe.scalalogging.LazyLogging
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}

class TaskController[F[_]: Async](authEndpointBuilder: AuthorizedEndpointBuilder[F], service: TaskService[F])
    extends LazyLogging {

  private val endpoint: PartialServerEndpoint[UsernamePassword, AuthSession, Unit, AppError, Unit, Any, F] =
    authEndpointBuilder.authorizedEndpoint.in("tasks")

  lazy val getTasksEndpoint: PartialServerEndpoint[
    UsernamePassword,
    AuthSession,
    (Option[Boolean], Option[String]),
    AppError,
    List[TaskResponse],
    Any,
    F
  ] =
    endpoint.get
      .in(query[Option[Boolean]]("finished").description("filtering by is_finished status"))
      .in(query[Option[String]]("filter_by").description("filtering by matching a substring to task's description"))
      .out(jsonBody[List[TaskResponse]])
      .description("Get all tasks")

  lazy val getTaskEndpoint: PartialServerEndpoint[UsernamePassword, AuthSession, Int, AppError, TaskResponse, Any, F] =
    endpoint.get
      .in(path[Int]("id"))
      .out(jsonBody[TaskResponse])
      .description("Get task by id")

  lazy val postTaskEndpoint: PartialServerEndpoint[
    UsernamePassword,
    AuthSession,
    TaskRequest,
    AppError,
    TaskResponse,
    Any,
    F
  ] =
    endpoint.post
      .in(jsonBody[TaskRequest])
      .out(jsonBody[TaskResponse])
      .description("Create a new task")

  lazy val updateTaskEndpoint: PartialServerEndpoint[
    UsernamePassword,
    AuthSession,
    (Int, TaskRequest),
    AppError,
    Unit,
    Any,
    F
  ] =
    endpoint.put
      .in(path[Int]("id"))
      .in(jsonBody[TaskRequest])
      .description("Update task by id")

  lazy val deleteTaskEndpoint: PartialServerEndpoint[UsernamePassword, AuthSession, Int, AppError, Unit, Any, F] =
    endpoint.delete
      .in(path[Int]("id"))
      .description("Delete task by id")

  def serverEndpoints: List[ServerEndpoint[Any, F]] =
    List(
      getTasksEndpoint.serverLogic { session: AuthSession =>
        {
          case (optFinished, optFiler) => service.getTasksLogic(session.userId, optFinished, optFiler)
        }
      },
      getTaskEndpoint.serverLogic { session: AuthSession => taskId: Int =>
        service.getTaskLogic(session.userId, taskId)
      },
      postTaskEndpoint.serverLogic { session: AuthSession => task: TaskRequest =>
        service.insertTaskLogic(session.userId, task)
      },
      updateTaskEndpoint.serverLogic { session: AuthSession =>
        {
          case (taskId, newTask) => service.updateTaskLogic(session.userId, taskId, newTask)
        }
      },
      deleteTaskEndpoint.serverLogic { session: AuthSession => taskId: Int =>
        service.deleteTaskLogic(session.userId, taskId)
      }
    )
}

object TaskController {
  def apply[F[_]: Async](
      authEndpointBuilder: AuthorizedEndpointBuilder[F],
      service: TaskService[F]
  ): F[TaskController[F]] =
    Sync[F].delay(new TaskController[F](authEndpointBuilder, service))
}
