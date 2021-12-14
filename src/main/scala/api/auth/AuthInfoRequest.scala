package api.auth

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema

case class AuthInfoRequest(username: String, password: String)
object AuthInfoRequest {
  implicit val encoder: Encoder[AuthInfoRequest] = deriveEncoder[AuthInfoRequest]
  implicit val decoder: Decoder[AuthInfoRequest] = deriveDecoder[AuthInfoRequest]
  implicit val schema: Schema[AuthInfoRequest]   = Schema.derived[AuthInfoRequest]
}
