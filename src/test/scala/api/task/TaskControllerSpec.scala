package api.task

import api.auth.{AuthService, AuthSession, AuthorizedEndpointBuilder}
import api.errors.{NotFound, Unauthorized}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.Method.{DELETE, GET, POST, PUT}
import org.http4s._
import io.circe.syntax._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.server.http4s.Http4sServerInterpreter
import io.circe.generic.auto._

class TaskControllerSpec extends AnyFlatSpec with Matchers with MockFactory {
  private val taskServiceMock: TaskService[IO] = mock[TaskService[IO]]
  private val authServiceMock: AuthService[IO] = mock[AuthService[IO]]
  private val authEndpointBuilder              = AuthorizedEndpointBuilder[IO](authServiceMock).unsafeRunSync()
  private val taskController                   = TaskController[IO](authEndpointBuilder, taskServiceMock)

  "TaskController" should "get all tasks" in {
    val userId = 1
    val tasks = List(
      TaskResponse(1, "d1", false),
      TaskResponse(2, "d2", false),
      TaskResponse(3, "d2", false)
    )
    val username    = "shrek"
    val password    = "kek"
    val authHeader  = Headers(Authorization(BasicCredentials(username, password)))
    val authSession = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.getTasksLogic _).expects(userId, *, *).returns(IO(Right(tasks)))

    val response = serve(
      Request(GET, Uri.unsafeFromString(s"/api/tasks")).withHeaders(authHeader)
    )
    response.status shouldBe Status.Ok
    response.bodyText.compile.string.unsafeRunSync() shouldBe tasks.asJson.noSpaces
  }

  it should "not accept unauthorized requests" in {
    val response = serve(Request(GET, uri"/api/tasks"))
    response.status shouldBe Status.Unauthorized
  }

  it should "not accept accepts requests with incorrect auth info" in {
    val username          = "shrek"
    val incorrectPassword = "lol"
    val authHeader        = Headers(Authorization(BasicCredentials(username, incorrectPassword)))
    val expected          = Unauthorized("authentication failed")
    (authServiceMock.authorize _).expects(username, incorrectPassword).returns(IO(Left(expected)))

    val response = serve(Request(GET, uri"/api/tasks").withHeaders(authHeader))
    response.status shouldBe Status.Unauthorized
    response.bodyText.compile.string.unsafeRunSync() shouldBe expected.asJson.noSpaces
  }

  it should "return task by id if present in db" in {
    val userId          = 1
    val taskId          = 1
    val taskDescription = "description"
    val taskIsFinished  = false
    val taskResponse    = TaskResponse(taskId, taskDescription, taskIsFinished)
    val username        = "shrek"
    val password        = "kek"
    val authHeader      = Headers(Authorization(BasicCredentials(username, password)))
    val authSession     = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.getTaskLogic _).expects(userId, taskId).returns(IO(Right(taskResponse)))

    val response = serve(Request(GET, Uri.unsafeFromString(s"/api/tasks/$taskId")).withHeaders(authHeader))
    response.status shouldBe Status.Ok
    response.bodyText.compile.string.unsafeRunSync() shouldBe taskResponse.asJson.noSpaces
  }

  it should "not return task by id if not present in db" in {
    val userId           = 1
    val taskId           = 1
    val expectedResponse = NotFound("task not found")
    val username         = "shrek"
    val password         = "kek"
    val authHeader       = Headers(Authorization(BasicCredentials(username, password)))
    val authSession      = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.getTaskLogic _).expects(userId, taskId).returns(IO(Left(expectedResponse)))

    val response = serve(Request(GET, Uri.unsafeFromString(s"/api/tasks/$taskId")).withHeaders(authHeader))
    response.status shouldBe Status.NotFound
    response.bodyText.compile.string.unsafeRunSync() shouldBe expectedResponse.asJson.noSpaces
  }

  it should "add new task if correct body supplied" in {
    val userId          = 1
    val taskId          = 1
    val taskDescription = "description"
    val taskIsFinished  = false
    val taskRequest     = TaskRequest(taskDescription, taskIsFinished)
    val taskResponse    = TaskResponse(taskId, taskDescription, taskIsFinished)
    val username        = "shrek"
    val password        = "kek"
    val authHeader      = Headers(Authorization(BasicCredentials(username, password)))
    val authSession     = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.insertTaskLogic _).expects(userId, taskRequest).returns(IO(Right(taskResponse)))

    val response = serve(
      Request(POST, Uri.unsafeFromString(s"/api/tasks")).withHeaders(authHeader).withEntity(taskRequest.asJson.noSpaces)
    )
    response.status shouldBe Status.Ok
    response.bodyText.compile.string.unsafeRunSync() shouldBe taskResponse.asJson.noSpaces
  }

  it should "not add new task if correct body not supplied" in {
    val userId      = 1
    val username    = "shrek"
    val password    = "kek"
    val authHeader  = Headers(Authorization(BasicCredentials(username, password)))
    val authSession = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))

    val response = serve(
      Request(POST, Uri.unsafeFromString(s"/api/tasks")).withHeaders(authHeader)
    )
    response.status shouldBe Status.BadRequest
  }

  it should "update task by id if correct stuff is supplied" in {
    val userId          = 1
    val taskId          = 1
    val taskDescription = "description"
    val taskIsFinished  = false
    val taskRequest     = TaskRequest(taskDescription, taskIsFinished)
    val username        = "shrek"
    val password        = "kek"
    val authHeader      = Headers(Authorization(BasicCredentials(username, password)))
    val authSession     = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.updateTaskLogic _).expects(userId, taskId, taskRequest).returns(IO(Right(())))

    val response = serve(
      Request(PUT, Uri.unsafeFromString(s"/api/tasks/$taskId"))
        .withHeaders(authHeader)
        .withEntity(taskRequest.asJson.noSpaces)
    )
    response.status shouldBe Status.Ok
    response.bodyText.compile.string.unsafeRunSync() shouldBe ""
  }

  it should "return 404 if no task was found in update request" in {
    val userId           = 1
    val taskId           = 1
    val taskDescription  = "description"
    val taskIsFinished   = false
    val taskRequest      = TaskRequest(taskDescription, taskIsFinished)
    val expectedResponse = NotFound("task not found")
    val username         = "shrek"
    val password         = "kek"
    val authHeader       = Headers(Authorization(BasicCredentials(username, password)))
    val authSession      = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.updateTaskLogic _).expects(userId, taskId, taskRequest).returns(IO(Left(expectedResponse)))

    val response = serve(
      Request(PUT, Uri.unsafeFromString(s"/api/tasks/$taskId"))
        .withHeaders(authHeader)
        .withEntity(taskRequest.asJson.noSpaces)
    )
    response.status shouldBe Status.NotFound
    response.bodyText.compile.string.unsafeRunSync() shouldBe expectedResponse.asJson.noSpaces
  }

  it should "return 404 if no task was found in delete request" in {
    val userId      = 1
    val taskId      = 1
    val username    = "shrek"
    val password    = "kek"
    val authHeader  = Headers(Authorization(BasicCredentials(username, password)))
    val authSession = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.deleteTaskLogic _).expects(userId, taskId).returns(IO(Right(())))

    val response = serve(
      Request(DELETE, Uri.unsafeFromString(s"/api/tasks/$taskId")).withHeaders(authHeader)
    )
    response.status shouldBe Status.Ok
    response.bodyText.compile.string.unsafeRunSync() shouldBe ""
  }

  it should "delete task by id if correct stuff is supplied" in {
    val userId           = 1
    val taskId           = 1
    val expectedResponse = NotFound("task not found")
    val username         = "shrek"
    val password         = "kek"
    val authHeader       = Headers(Authorization(BasicCredentials(username, password)))
    val authSession      = AuthSession(username, userId)
    (authServiceMock.authorize _).expects(username, password).returns(IO(Right(authSession)))
    (taskServiceMock.deleteTaskLogic _).expects(userId, taskId).returns(IO(Left(expectedResponse)))

    val response = serve(
      Request(DELETE, Uri.unsafeFromString(s"/api/tasks/$taskId")).withHeaders(authHeader)
    )
    response.status shouldBe Status.NotFound
    response.bodyText.compile.string.unsafeRunSync() shouldBe expectedResponse.asJson.noSpaces
  }

  private def serve(request: Request[IO]): Response[IO] = {
    taskController
      .flatMap(controller => Http4sServerInterpreter[IO]().toRoutes(controller.serverEndpoints).orNotFound.run(request))
      .unsafeRunSync()
  }
}
