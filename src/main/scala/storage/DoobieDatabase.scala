package storage

import cats.effect.kernel.Async
import cats.effect.{Resource, Sync}
import configs.DoobieConfig
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object DoobieDatabase {
  def transactor[F[_]: Async](
      config: DoobieConfig,
      executionContext: ExecutionContext
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      config.driver,
      config.url,
      config.user,
      config.password,
      executionContext
    )

  def performMigrations[F[_]: Sync](transactor: HikariTransactor[F]): F[Unit] =
    transactor.configure { ds =>
      Sync[F].delay {
        Flyway.configure().dataSource(ds).load().migrate()
        ()
      }
    }
}
