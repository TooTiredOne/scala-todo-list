package api.auth

import api.errors.{AppError, BadRequest, Unauthorized}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import cats.syntax.either._
import org.scalatest.flatspec.AnyFlatSpec
import storage.auth.error.UserDbError
import storage.auth.{UserDto, UserStorage}
import tsec.passwordhashers.jca.SCrypt

class AuthServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  private val userStorageMock = mock[UserStorage[IO]]
  private val authService     = AuthService[IO](userStorageMock).unsafeRunSync()
  "AuthService" should "register a new user" in {
    val username     = "username1"
    val password     = "123456"
    val passwordHash = "kek"
    val userId       = 1
    (userStorageMock.insertUser _)
      .expects(username, *)
      .returning(IO.pure(Right(UserDto(userId, username, passwordHash))))

    val test = for {
      response <- authService.register(username, password)
    } yield {
      response shouldBe ().asRight[AppError]
    }
    test.unsafeRunSync()
  }

  it should "return error if the username is taken" in {
    val username = "username1"
    val password = "123456"
    (userStorageMock.insertUser _)
      .expects(username, *)
      .returning(IO.pure(Left(UserDbError.UniqueValidationError(""))))

    val test = for {
      response <- authService.register(username, password)
    } yield {
      response shouldBe BadRequest("user with such name already exists").asLeft[Unit]
    }
    test.unsafeRunSync()
  }

  it should "authorize user given correct values" in {
    val username = "username1"
    val userId   = 1
    val password = "123456"
    val test = for {
      passwordHash <- SCrypt.hashpw[IO](password)
      userDto      = UserDto(userId, username, passwordHash)
      _ <- IO.pure(
            (userStorageMock.getUser _)
              .expects(username)
              .returning(IO.pure(Some(userDto).asRight[UserDbError]))
          )
      response <- authService.authorize(username, password)
    } yield {
      response shouldBe AuthSession(username, userId).asRight[AppError]
    }
    test.unsafeRunSync()
  }

  it should "not authorize user given incorrect password" in {
    val username      = "username1"
    val userId        = 1
    val passwordGiven = "12345"
    val passwordReal  = "123456"
    val test = for {
      passwordHash <- SCrypt.hashpw[IO](passwordReal)
      userDto      = UserDto(userId, username, passwordHash)
      _ <- IO.pure(
            (userStorageMock.getUser _)
              .expects(username)
              .returning(IO.pure(Some(userDto).asRight[UserDbError]))
          )
      response <- authService.authorize(username, passwordGiven)
    } yield {
      response shouldBe Unauthorized("authentication failed").asLeft[AuthSession]
    }
    test.unsafeRunSync()
  }

  it should "not authorize user given incorrect username" in {
    val usernameIncorrect = "usrname1"
    val password          = "123456"
    (userStorageMock.getUser _)
      .expects(usernameIncorrect)
      .returning(IO.pure(None.asRight[UserDbError]))
    val test = for {
      response <- authService.authorize(usernameIncorrect, password)
    } yield {
      response shouldBe Unauthorized("authentication failed").asLeft[AuthSession]
    }
    test.unsafeRunSync()
  }

}
