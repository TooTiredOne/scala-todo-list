package api.task

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

case class TaskRequest(description: String, isFinished: Boolean)
object TaskRequest {
  implicit val encoder: Encoder[TaskRequest] = deriveEncoder[TaskRequest]
  implicit val decoder: Decoder[TaskRequest] = deriveDecoder[TaskRequest]
  implicit val schema: Schema[TaskRequest]   = Schema.derived[TaskRequest]
}
