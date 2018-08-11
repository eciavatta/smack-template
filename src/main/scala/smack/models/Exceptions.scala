package smack.models

case class TestException(message: String) extends Exception(message)

case object SerializationException extends Exception
