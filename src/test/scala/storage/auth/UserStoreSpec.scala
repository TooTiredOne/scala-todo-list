package storage.auth

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import storage.auth.error.UserDbError

class UserStoreSpec extends AnyFlatSpec with IOChecker with Matchers with BeforeAndAfter {
  override def transactor: doobie.Transactor[IO] = {
    Transactor.fromDriverManager[IO]("org.h2.Driver", "jdbc:h2:mem:test_user;DB_CLOSE_DELAY=-1")
  }

  before {
    sql"""create table if not exists users (
         |    id serial primary key,
         |    username varchar(30) unique not null,
         |    password_hash varchar not null
         |);""".stripMargin.update.run.transact(transactor).unsafeRunSync()
  }

  after {
    sql"""
         drop table users;
       """.update.run.transact(transactor).unsafeRunSync()
  }

  val userStorage: UserStorageImpl[IO] = UserStorage[IO](transactor).unsafeRunSync()

  "Queries" should "be correct" in {
    check(queries.insertUser("name", "hash"))
    check(queries.getUser("name"))
  }

  "UserStorage" should "get user by username if exists" in {
    val username     = "shrek"
    val passwordHash = "kek"
    sql"insert into users (username, password_hash) values ($username, $passwordHash)".update.run
      .transact(transactor)
      .unsafeRunSync()

    userStorage
      .getUser(username)
      .map(res => res shouldBe Right(Some(UserDto(1, username, passwordHash))))
      .unsafeRunSync()
  }

  it should "not get user by username if doesn't exist" in {
    val username      = "shrek"
    val passwordHash  = "kek"
    val otherUsername = "Buba"
    sql"insert into users (username, password_hash) values ($username, $passwordHash)".update.run
      .transact(transactor)
      .unsafeRunSync()

    userStorage
      .getUser(otherUsername)
      .map(res => res shouldBe Right(None))
      .unsafeRunSync()
  }

  it should "insert new user with unique name" in {
    val username     = "shrek"
    val passwordHash = "kek"

    userStorage
      .insertUser(username, passwordHash)
      .map(res => res shouldBe Right(UserDto(1, username, passwordHash)))
      .unsafeRunSync()
  }

  it should "not insert new user with taken name" in {
    val username     = "shrek"
    val passwordHash = "kek"

    sql"insert into users (username, password_hash) values ($username, $passwordHash)".update.run
      .transact(transactor)
      .unsafeRunSync()

    userStorage
      .insertUser(username, passwordHash)
      .map(res => res shouldBe Left(UserDbError.UniqueValidationError("username must be unique")))
      .unsafeRunSync()
  }
}
