package api.task

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

case class TaskResponse(id: Int, description: String, isFinished: Boolean)
object TaskResponse {
  implicit val encoder: Encoder[TaskResponse] = deriveEncoder[TaskResponse]
  implicit val decoder: Decoder[TaskResponse] = deriveDecoder[TaskResponse]
  implicit val schema: Schema[TaskResponse]   = Schema.derived[TaskResponse]
}
