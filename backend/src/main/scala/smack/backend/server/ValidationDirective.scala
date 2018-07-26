package smack.backend.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, ValidationRejection}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * Taken from https://github.com/Fruzenshtein/akka-http-validation for request validation
  */
object ValidationDirective extends SprayJsonSupport with DefaultJsonProtocol {

  import akka.http.scaladsl.server.Rejection

  def validateModel[T, M <: Any](model: T, rules: FieldRule[M]*): Directive1[T] = {
    val errorsSet: mutable.Set[FieldErrorInfo] = mutable.Set[FieldErrorInfo]()
    val keyValuePairs: Seq[(String, M)] = caseClassFields(model.asInstanceOf[AnyRef])
    Try {
      rules.map { rule =>
        keyValuePairs.find(_._1 == rule.fieldName) match {
          case None => throw new IllegalArgumentException(s"No such field for validation: ${rule.fieldName}")
          case Some(pair) => if (rule.isInvalid(pair._2)) errorsSet += FieldErrorInfo(rule.fieldName, rule.errorMsg)
        }
      }
      errorsSet.toSet[FieldErrorInfo]
    } match {
      case Success(set) => if (set.isEmpty) provide(model) else reject(ModelValidationRejection(set))
      case Failure(ex) => reject(ValidationRejection(ex.getMessage))
    }
  }

  private def caseClassFields[M <: Any](obj: AnyRef): Seq[(String, M)] = {
    val metaClass = obj.getClass
    metaClass.getDeclaredFields.map {
      field => {
        field.setAccessible(true)
        (field.getName, field.get(obj).asInstanceOf[M])
      }
    }
  }

  case class FieldRule[-M](fieldName: String, isInvalid: M => Boolean, errorMsg: String)

  implicit val validatedFieldFormat: RootJsonFormat[FieldErrorInfo] = jsonFormat2(FieldErrorInfo)

  final case class FieldErrorInfo(name: String, error: String)

  final case class ModelValidationRejection(invalidFields: Set[FieldErrorInfo]) extends Rejection

}
