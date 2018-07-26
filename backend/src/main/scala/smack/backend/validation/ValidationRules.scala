package smack.backend.validation

import smack.backend.server.ValidationDirective.FieldRule

object StringLengthRule {
  def apply(fieldName: String, length: Int): FieldRule[String] = {
    FieldRule(fieldName, (s: String) => s.length < length, s"$fieldName length must be greater than $length")
  }
}

object EmailRule {
  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private def check(s: String): Boolean = s match {
    case e if e.trim.isEmpty => false
    case e if emailRegex.findFirstMatchIn(e).isDefined => true
    case _ => false
  }

  def apply(fieldName: String): FieldRule[String] = {
    FieldRule(fieldName, (email: String) => !check(email), s"field $fieldName is not a valid email")
  }
}
