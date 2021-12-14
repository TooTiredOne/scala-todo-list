package api.task

import api.errors.{AppError, InternalServerError, NotFound}
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import storage.task.TaskStorage
import io.scalaland.chimney.dsl._

trait TaskService[F[_]] {
  def getTasksLogic(
      userId: Int,
      isFinished: Option[Boolean],
      filter: Option[String]
  ): F[Either[AppError, List[TaskResponse]]]
  def getTaskLogic(userId: Int, taskId: Int): F[Either[AppError, TaskResponse]]
  def insertTaskLogic(userId: Int, task: TaskRequest): F[Either[AppError, TaskResponse]]
  def updateTaskLogic(userId: Int, taskId: Int, task: TaskRequest): F[Either[AppError, Unit]]
  def deleteTaskLogic(userId: Int, taskId: Int): F[Either[AppError, Unit]]
}
object TaskService {
  def apply[F[_]: Sync](storage: TaskStorage[F]): F[TaskServiceImpl[F]] =
    Sync[F].delay(new TaskServiceImpl[F](storage))
}

class TaskServiceImpl[F[_]: Sync](storage: TaskStorage[F]) extends TaskService[F] {
  def getTasksLogic(
      userId: Int,
      isFinished: Option[Boolean],
      filter: Option[String]
  ): F[Either[AppError, List[TaskResponse]]] = {
    storage.getTasks(userId, isFinished, filter).map {
      case Right(lst) => lst.transformInto[List[TaskResponse]].asRight[AppError]
      case Left(err)  => InternalServerError(err.cause).asLeft[List[TaskResponse]]
    }
  }
  def getTaskLogic(userId: Int, taskId: Int): F[Either[AppError, TaskResponse]] = {
    storage.getTask(userId, taskId).map {
      case Right(optTaskDto) =>
        optTaskDto match {
          case Some(taskDto) => taskDto.transformInto[TaskResponse].asRight[AppError]
          case None          => NotFound("task not found").asLeft[TaskResponse]
        }
      case Left(err) => InternalServerError(err.cause).asLeft[TaskResponse]
    }
  }
  def insertTaskLogic(userId: Int, task: TaskRequest): F[Either[AppError, TaskResponse]] = {
    storage.insertTask(userId, task).map {
      case Right(taskDto) => taskDto.transformInto[TaskResponse].asRight[AppError]
      case Left(err)      => InternalServerError(err.cause).asLeft[TaskResponse]
    }
  }
  def updateTaskLogic(userId: Int, taskId: Int, task: TaskRequest): F[Either[AppError, Unit]] = {
    storage
      .updateTask(userId, taskId, task)
      .map {
        case Right(affectedCount) =>
          if (affectedCount == 0) NotFound("task not found").asLeft[Unit]
          else ().asRight[AppError]
        case Left(err) => InternalServerError(err.cause).asLeft[Unit]
      }
  }
  def deleteTaskLogic(userId: Int, taskId: Int): F[Either[AppError, Unit]] = {
    storage
      .deleteTask(userId, taskId)
      .map {
        case Right(affectedCount) =>
          affectedCount.asRight[AppError]
          if (affectedCount == 0) NotFound("task not found").asLeft[Unit]
          else ().asRight[AppError]
        case Left(err) => InternalServerError(err.cause).asLeft[Unit]
      }
  }
}
