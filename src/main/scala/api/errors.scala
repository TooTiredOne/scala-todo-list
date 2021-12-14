package api

object errors {
  sealed trait AppError {
    def message: String
  }
  case class NotFound(message: String)            extends AppError
  case class InternalServerError(message: String) extends AppError
  case class Unauthorized(message: String)        extends AppError
  case class BadRequest(message: String)          extends AppError
}
