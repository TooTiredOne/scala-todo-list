package storage.auth

object error {
  sealed trait UserDbError {
    def cause: String
  }
  object UserDbError {
    case class UniqueValidationError(cause: String) extends UserDbError
    case class GeneralError(cause: String)          extends UserDbError
  }
}
