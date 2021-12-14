package storage.task

object error {
  sealed trait TaskDbError {
    def cause: String
  }
  object TaskDbError {
    case class GeneralError(cause: String) extends TaskDbError
  }
}
