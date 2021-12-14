package storage.task

import api.task.TaskRequest
import cats.effect.{MonadCancel, Sync}
import cats.syntax.either._
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import doobie.util.fragments.whereAndOpt
import storage.task.error.TaskDbError

trait TaskStorage[F[_]] {
  def getTasks(userId: Int, isFinished: Option[Boolean], filter: Option[String]): F[Either[TaskDbError, List[TaskDto]]]
  def getTask(userId: Int, taskId: Int): F[Either[TaskDbError, Option[TaskDto]]]
  def insertTask(userId: Int, task: TaskRequest): F[Either[TaskDbError, TaskDto]]
  def updateTask(userId: Int, taskId: Int, task: TaskRequest): F[Either[TaskDbError, Int]]
  def deleteTask(userId: Int, taskId: Int): F[Either[TaskDbError, Int]]
}
object TaskStorage {
  def apply[F[_]: Sync](
      transactor: Transactor[F]
  )(implicit monadCancel: MonadCancel[F, Throwable]): F[TaskStorageImpl[F]] =
    Sync[F].delay(new TaskStorageImpl[F](transactor))
}

final class TaskStorageImpl[F[_]](transactor: Transactor[F])(implicit monadCancel: MonadCancel[F, Throwable])
    extends TaskStorage[F] {
  override def getTasks(
      userId: Int,
      isFinished: Option[Boolean],
      filter: Option[String]
  ): F[Either[TaskDbError, List[TaskDto]]] = {
    queries
      .getTasks(userId, isFinished, filter)
      .stream
      .compile
      .toList
      .transact(transactor)
      .attemptSql
      .map(_.leftMap(ex => TaskDbError.GeneralError(ex.getMessage)))
  }

  override def getTask(userId: Int, taskId: Int): F[Either[TaskDbError, Option[TaskDto]]] =
    queries
      .getTask(userId, taskId)
      .option
      .transact(transactor)
      .attemptSql
      .map(_.leftMap(ex => TaskDbError.GeneralError(ex.getMessage)))

  override def insertTask(userId: Int, task: TaskRequest): F[Either[TaskDbError, TaskDto]] =
    queries
      .insertTask(userId, task)
      .withUniqueGeneratedKeys[TaskDto]("id", "user_id", "description", "is_finished")
      .transact(transactor)
      .attemptSql
      .map(_.leftMap(ex => TaskDbError.GeneralError(ex.getMessage)))

  override def updateTask(userId: Int, taskId: Int, task: TaskRequest): F[Either[TaskDbError, Int]] =
    queries
      .updateTask(userId, taskId, task)
      .run
      .transact(transactor)
      .attemptSql
      .map(_.leftMap(ex => TaskDbError.GeneralError(ex.getMessage)))

  override def deleteTask(userId: Int, taskId: Int): F[Either[TaskDbError, Int]] =
    queries
      .deleteTask(userId, taskId)
      .run
      .transact(transactor)
      .attemptSql
      .map(_.leftMap(ex => TaskDbError.GeneralError(ex.getMessage)))
}

object queries {
  def getTasks(userId: Int, isFinished: Option[Boolean], filter: Option[String]): doobie.Query0[TaskDto] = {
    val statusFilter    = isFinished.map(s => fr"is_finished = $s")
    val substringFilter = filter.map(s => fr"description like '%' || $s || '%'")
    val userIdFilter    = Some(userId).map(i => fr"user_id = $i")
    val q = fr"select id, user_id, description, is_finished from tasks" ++
      whereAndOpt(statusFilter, substringFilter, userIdFilter)

    q.query[TaskDto]
  }

  def getTask(userId: Int, taskId: Int): doobie.Query0[TaskDto] =
    sql"select id, user_id, description, is_finished from tasks where id = $taskId and user_id = $userId"
      .query[TaskDto]

  def insertTask(userId: Int, task: TaskRequest): doobie.Update0 =
    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${task.description}, ${task.isFinished})".update

  def updateTask(userId: Int, taskId: Int, task: TaskRequest): doobie.Update0 =
    sql"update tasks set description = ${task.description}, is_finished = ${task.isFinished} where id = $taskId and user_id = $userId".update

  def deleteTask(userId: Int, taskId: Int): doobie.Update0 =
    sql"delete from tasks where id = $taskId and user_id = $userId".update

}
