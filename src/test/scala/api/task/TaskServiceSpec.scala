package api.task

import api.errors.NotFound
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import storage.task.{TaskDto, TaskStorage}
import cats.syntax.either._

class TaskServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  private val taskStorageMock = mock[TaskStorage[IO]]
  private val taskService     = TaskService[IO](taskStorageMock).unsafeRunSync()

  "TaskService" should "return task by id for user with correct id" in {
    val taskId       = 1
    val userId       = 1
    val description  = "kek"
    val isFinished   = false
    val taskDto      = TaskDto(taskId, userId, description, isFinished)
    val taskResponse = TaskResponse(taskId, description, isFinished)
    (taskStorageMock.getTask _).expects(userId, taskId).returning(IO.pure(Right(Some(taskDto))))

    taskService.getTaskLogic(userId, taskId).map(res => res shouldBe Right(taskResponse)).unsafeRunSync()
  }

  it should "not return task by id for user with incorrect id" in {
    val taskId = 1
    val userId = 1
    (taskStorageMock.getTask _).expects(userId, taskId).returns(IO.pure(Right(None)))

    taskService.getTaskLogic(userId, taskId).map(res => res shouldBe Left(NotFound("task not found"))).unsafeRunSync()
  }

  it should "return all filtered tasks for user with correct id" in {
    val userId   = 1
    val taskDto1 = TaskDto(1, userId, "description 1", false)
    val taskDto2 = TaskDto(2, userId, "description 2", false)
    val taskDto3 = TaskDto(3, userId, "description 3", true)

    val inputExpectedOutput = List(
      ((userId, None, None), List(taskDto1, taskDto2, taskDto3)),
      ((userId, Some(false), None), List(taskDto1, taskDto2)),
      ((userId, Some(true), None), List(taskDto3)),
      ((userId, Some(false), Some("3")), List()),
      ((userId, Some(false), Some("description")), List(taskDto1, taskDto2)),
      ((userId, Some(false), Some("description")), List(taskDto1, taskDto2))
    )

    inputExpectedOutput.foreach {
      case (input, output) =>
        (taskStorageMock.getTasks _).expects(input._1, input._2, input._3).returning(IO.pure(Right(output)))
    }

    inputExpectedOutput.foreach {
      case (input, output) =>
        taskService
          .getTasksLogic(input._1, input._2, input._3)
          .map(res => res shouldBe Right(output.map(dto => TaskResponse(dto.id, dto.description, dto.isFinished))))
          .unsafeRunSync()
    }
  }

  it should "insert new task" in {
    val userId       = 1
    val taskId       = 2
    val description  = "kek"
    val isFinished   = false
    val taskRequest  = TaskRequest(description, isFinished)
    val taskResponse = TaskResponse(taskId, description, isFinished)
    val taskDto      = TaskDto(taskId, userId, description, isFinished)
    (taskStorageMock.insertTask _).expects(userId, taskRequest).returning(IO.pure(Right(taskDto)))

    taskService.insertTaskLogic(userId, taskRequest).map(res => res shouldBe Right(taskResponse)).unsafeRunSync()
  }

  it should "update task if correct arguments provided" in {
    val userId      = 1
    val taskId      = 2
    val description = "kek"
    val isFinished  = false
    val taskRequest = TaskRequest(description, isFinished)
    (taskStorageMock.updateTask _).expects(userId, taskId, taskRequest).returning(IO.pure(Right(1)))

    taskService.updateTaskLogic(userId, taskId, taskRequest).map(res => res shouldBe Right(())).unsafeRunSync()
  }

  it should "not update task if userId or taskId incorrect" in {
    val thisUserId  = 1
    val otherTaskId = 2
    val description = "kek"
    val isFinished  = false
    val taskRequest = TaskRequest(description, isFinished)

    (taskStorageMock.updateTask _).expects(thisUserId, otherTaskId, taskRequest).returning(IO.pure(Right(0)))

    taskService
      .updateTaskLogic(thisUserId, otherTaskId, taskRequest)
      .map(res => res shouldBe NotFound("task not found").asLeft)
      .unsafeRunSync()
  }

  it should "delete task if correct arguments provided" in {
    val userId = 1
    val taskId = 2
    (taskStorageMock.deleteTask _).expects(userId, taskId).returning(IO.pure(Right(1)))

    taskService.deleteTaskLogic(userId, taskId).map(res => res shouldBe Right(())).unsafeRunSync()
  }

  it should "not delete task if userId or taskId incorrect" in {
    val thisUserId  = 1
    val otherTaskId = 2

    (taskStorageMock.deleteTask _).expects(thisUserId, otherTaskId).returning(IO.pure(Right(0)))

    taskService
      .deleteTaskLogic(thisUserId, otherTaskId)
      .map(res => res shouldBe NotFound("task not found").asLeft)
      .unsafeRunSync()
  }
}
