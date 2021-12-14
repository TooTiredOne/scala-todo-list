package api

import api.errors.{AppError, BadRequest, InternalServerError, NotFound, Unauthorized}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object endpoints {
  val baseEndpoint: Endpoint[Unit, Unit, AppError, Unit, Any] =
    endpoint
      .errorOut(
        oneOf[AppError](
          oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
          oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[InternalServerError])),
          oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized])),
          oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest]))
        )
      )
}
