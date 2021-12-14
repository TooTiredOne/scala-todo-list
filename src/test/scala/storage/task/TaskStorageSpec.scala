package storage.task

import api.task.TaskRequest
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TaskStorageSpec extends AnyFlatSpec with IOChecker with Matchers with BeforeAndAfter {

  override def transactor: doobie.Transactor[IO] = {
    Transactor.fromDriverManager[IO]("org.h2.Driver", "jdbc:h2:mem:test_task;DB_CLOSE_DELAY=-1")
  }
  private val taskStorage: TaskStorageImpl[IO] = TaskStorage[IO](transactor).unsafeRunSync()

  before {
    val createUsersSql =
      sql"""create table if not exists users (
           |    id serial primary key,
           |    username varchar(30) unique not null,
           |    password_hash varchar not null
           |)""".stripMargin

    val createTablesSql =
      sql"""create table if not exists tasks (
           |    id serial primary key,
           |    user_id integer not null,
           |    description varchar(256) not null,
           |    is_finished boolean not null,
           |    foreign key (user_id) references users (id)
           |)""".stripMargin

    createUsersSql.update.run.transact(transactor).unsafeRunSync()
    createTablesSql.update.run.transact(transactor).unsafeRunSync()
  }

  after {
    sql"drop table tasks, users;".update.run.transact(transactor).unsafeRunSync()
  }

  "Queries" should "be correct" in {
    val taskRequest = TaskRequest("description", isFinished = false)

    check(queries.getTask(1, 1))
    check(queries.getTasks(1, Some(true), None))
    check(queries.insertTask(1, taskRequest))
    check(queries.updateTask(1, 1, taskRequest))
    check(queries.deleteTask(1, 1))
  }

  "TaskStorage" should "get task by id if exists" in {
    val taskId          = 1
    val userId          = 1
    val username        = "shrek"
    val userPass        = "kek"
    val taskDescription = "description"
    val taskIsFinished  = false
    val taskDto         = TaskDto(taskId, userId, taskDescription, taskIsFinished)

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${taskDescription}, ${taskIsFinished})".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .getTask(userId, taskId)
      .map(res => res shouldBe Right(Some(taskDto)))
      .unsafeRunSync()
  }

  it should "not get task by id if doesn't exists" in {
    val taskId          = 1
    val userId          = 1
    val username        = "shrek"
    val userPass        = "kek"
    val taskDescription = "description"
    val taskIsFinished  = false

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${taskDescription}, ${taskIsFinished})".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .getTask(userId, taskId + 1)
      .map(res => res shouldBe Right(None))
      .unsafeRunSync()

    taskStorage
      .getTask(userId + 1, taskId)
      .map(res => res shouldBe Right(None))
      .unsafeRunSync()
  }

  it should "get tasks applying filters" in {
    val userId1   = 1
    val username1 = "shrek1"
    val userId2   = 2
    val username2 = "shrek2"
    val userPass  = "kek"
    val tasks = List(
      TaskDto(1, userId1, "description 1", false),
      TaskDto(2, userId1, "description 2", false),
      TaskDto(3, userId1, "description 3", true),
      TaskDto(4, userId2, "description 4", false),
      TaskDto(5, userId2, "description 5", false)
    )

    sql"insert into users (username, password_hash) values ($username1, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()
    sql"insert into users (username, password_hash) values ($username2, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    tasks.foreach(dto =>
      sql"insert into tasks (user_id, description, is_finished) values (${dto.userId}, ${dto.description}, ${dto.isFinished})".update.run
        .transact(transactor)
        .unsafeRunSync()
    )

    taskStorage
      .getTasks(userId1, Some(false), None)
      .map(res => res shouldBe Right(tasks.slice(0, 2)))
      .unsafeRunSync()

    taskStorage
      .getTasks(userId1, None, Some("3"))
      .map(res => res shouldBe Right(List(tasks(2))))
      .unsafeRunSync()

    taskStorage
      .getTasks(userId2, None, Some("3"))
      .map(res => res shouldBe Right(List()))
      .unsafeRunSync()

    taskStorage
      .getTasks(userId2, None, None)
      .map(res => res shouldBe Right(tasks.slice(3, 5)))
      .unsafeRunSync()
  }

  it should "insert a task" in {
    val taskId          = 1
    val userId          = 1
    val username        = "shrek"
    val userPass        = "kek"
    val taskDescription = "description"
    val taskIsFinished  = false
    val taskRequest     = TaskRequest(taskDescription, taskIsFinished)
    val taskDto         = TaskDto(taskId, userId, taskDescription, taskIsFinished)

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .insertTask(userId, taskRequest)
      .map(res => res shouldBe Right(taskDto))
      .unsafeRunSync()

    val newDto: Option[TaskDto] =
      sql"select id, user_id, description, is_finished from tasks where id = $taskId and user_id = $userId"
        .query[TaskDto]
        .option
        .transact(transactor)
        .unsafeRunSync()

    newDto shouldBe Some(taskDto)
  }

  it should "update task" in {
    val taskId             = 1
    val userId             = 1
    val username           = "shrek"
    val userPass           = "kek"
    val taskDescription    = "description"
    val taskIsFinished     = false
    val newTaskDescription = "new description"
    val newTaskIsFinished  = true
    val taskRequest        = TaskRequest(newTaskDescription, newTaskIsFinished)

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${taskDescription}, ${taskIsFinished})".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .updateTask(userId, taskId, taskRequest)
      .map(res => res shouldBe Right(1))
      .unsafeRunSync()

    val newDto: Option[TaskDto] =
      sql"select id, user_id, description, is_finished from tasks where id = $taskId and user_id = $userId"
        .query[TaskDto]
        .option
        .transact(transactor)
        .unsafeRunSync()

    newDto shouldBe Some(TaskDto(taskId, userId, newTaskDescription, newTaskIsFinished))
  }

  it should "not update task" in {
    val taskId             = 1
    val userId             = 1
    val otherUserId        = 2
    val username           = "shrek"
    val userPass           = "kek"
    val taskDescription    = "description"
    val taskIsFinished     = false
    val newTaskDescription = "new description"
    val newTaskIsFinished  = true
    val taskRequest        = TaskRequest(newTaskDescription, newTaskIsFinished)

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${taskDescription}, ${taskIsFinished})".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .updateTask(otherUserId, taskId, taskRequest)
      .map(res => res shouldBe Right(0))
      .unsafeRunSync()

    val newDto: Option[TaskDto] =
      sql"select id, user_id, description, is_finished from tasks where id = $taskId and user_id = $userId"
        .query[TaskDto]
        .option
        .transact(transactor)
        .unsafeRunSync()

    newDto shouldBe Some(TaskDto(taskId, userId, taskDescription, taskIsFinished))
  }

  it should "delete task" in {
    val taskId          = 1
    val userId          = 1
    val username        = "shrek"
    val userPass        = "kek"
    val taskDescription = "description"
    val taskIsFinished  = false

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${taskDescription}, ${taskIsFinished})".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .deleteTask(userId, taskId)
      .map(res => res shouldBe Right(1))
      .unsafeRunSync()

    val newDto: Option[TaskDto] =
      sql"select id, user_id, description, is_finished from tasks where id = $taskId and user_id = $userId"
        .query[TaskDto]
        .option
        .transact(transactor)
        .unsafeRunSync()

    newDto shouldBe None
  }

  it should "not delete task" in {
    val taskId          = 1
    val userId          = 1
    val otherUserId     = 2
    val username        = "shrek"
    val userPass        = "kek"
    val taskDescription = "description"
    val taskIsFinished  = false

    sql"insert into users (username, password_hash) values ($username, $userPass)".update.run
      .transact(transactor)
      .unsafeRunSync()

    sql"insert into tasks (user_id, description, is_finished) values ($userId, ${taskDescription}, ${taskIsFinished})".update.run
      .transact(transactor)
      .unsafeRunSync()

    taskStorage
      .deleteTask(otherUserId, taskId)
      .map(res => res shouldBe Right(0))
      .unsafeRunSync()

    val newDto: Option[TaskDto] =
      sql"select id, user_id, description, is_finished from tasks where id = $taskId and user_id = $userId"
        .query[TaskDto]
        .option
        .transact(transactor)
        .unsafeRunSync()

    newDto shouldBe Some(TaskDto(taskId, userId, taskDescription, taskIsFinished))
  }
}
