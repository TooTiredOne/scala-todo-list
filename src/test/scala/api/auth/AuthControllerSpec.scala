package api.auth

import api.errors.BadRequest
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.Method.POST
import org.http4s._
import io.circe.syntax._
import org.http4s.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.server.http4s.Http4sServerInterpreter
import io.circe.generic.auto._

class AuthControllerSpec extends AnyFlatSpec with Matchers with MockFactory {
  private val authServiceMock: AuthService[IO]       = mock[AuthService[IO]]
  private val authController: IO[AuthController[IO]] = AuthController(authServiceMock)

  "AuthController" should "register new user with correct auth info" in {
    val username     = "username1"
    val password     = "123456"
    val registerJson = AuthInfoRequest(username, password).asJson.noSpaces
    (authServiceMock.register _).expects(username, password).returning(IO.pure(Right(())))

    val response = serve(Request(POST, uri"/api/register").withEntity(registerJson))
    response.status shouldBe Status.Ok
  }

  it should "not register new user if no auth info provided" in {
    val response = serve(Request(POST, uri"/api/register"))
    response.status shouldBe Status.BadRequest
  }

  it should "not register new user if username already taken" in {
    val username         = "username1"
    val password         = "123456"
    val registerJson     = AuthInfoRequest(username, password).asJson.noSpaces
    val expectedResponse = BadRequest("user with such name already exists")
    (authServiceMock.register _).expects(username, password).returning(IO.pure(Left(expectedResponse)))

    val response = serve(Request(POST, uri"/api/register").withEntity(registerJson))
    response.status shouldBe Status.BadRequest
    response.bodyText.compile.string.unsafeRunSync() shouldBe expectedResponse.asJson.noSpaces
  }

  private def serve(request: Request[IO]): Response[IO] = {
    authController
      .flatMap(controller => Http4sServerInterpreter[IO]().toRoutes(controller.serverEndpoints).orNotFound.run(request))
      .unsafeRunSync()
  }
}
