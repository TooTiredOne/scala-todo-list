package configs

import cats.effect.kernel.Sync
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

case class AppConfig(
    server: ServerConfig,
    doobie: DoobieConfig
)

object AppConfig {
  def defaultLoadF[F[_]: Sync]: F[AppConfig] =
    ConfigSource.default.loadF[F, AppConfig]
}
