package smack.commons.serialization

import akka.serialization.SerializerWithStringManifest
import smack.models.messages._
import smack.models.structures.ResponseStatus

class MessageSerializer extends SerializerWithStringManifest {

  private val serializableId = 9002
  override def identifier: Int = serializableId

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case TestRequestManifest => TestRequest.parseFrom(bytes)
      case TestResponseManifest => TestResponse.parseFrom(bytes)

      case FindUserRequestManifest => FindUserRequest.parseFrom(bytes)
      case FindUserResponseManifest => FindUserResponse.parseFrom(bytes)
      case CreateUserRequestManifest => CreateUserRequest.parseFrom(bytes)
      case CreateUserResponseManifest => CreateUserResponse.parseFrom(bytes)
      case UpdateUserRequestManifest => UpdateUserRequest.parseFrom(bytes)
      case UpdateUserResponseManifest => UpdateUserResponse.parseFrom(bytes)

      case ListSitesRequestManifest => ListSitesRequest.parseFrom(bytes)
      case ListSitesResponseManifest => ListSitesResponse.parseFrom(bytes)
      case FindSiteRequestManifest => FindSiteRequest.parseFrom(bytes)
      case FindSiteResponseManifest => FindSiteResponse.parseFrom(bytes)
      case CreateSiteRequestManifest => CreateSiteRequest.parseFrom(bytes)
      case CreateSiteResponseManifest => CreateSiteResponse.parseFrom(bytes)
      case UpdateSiteRequestManifest => UpdateSiteRequest.parseFrom(bytes)
      case UpdateSiteResponseManifest => UpdateSiteResponse.parseFrom(bytes)
      case DeleteSiteRequestManifest => DeleteSiteRequest.parseFrom(bytes)
      case DeleteSiteResponseManifest => DeleteSiteResponse.parseFrom(bytes)

      case TraceLogRequestManifest => TraceLogRequest.parseFrom(bytes)
      case TraceLogResponseManifest => TraceLogResponse.parseFrom(bytes)
    }

  override def manifest(o: AnyRef): String = o.getClass.getName
  final val TestRequestManifest = classOf[TestRequest].getName
  final val TestResponseManifest = classOf[TestResponse].getName

  final val FindUserRequestManifest = classOf[FindUserRequest].getName
  final val FindUserResponseManifest = classOf[FindUserResponse].getName
  final val CreateUserRequestManifest = classOf[CreateUserRequest].getName
  final val CreateUserResponseManifest = classOf[CreateUserResponse].getName
  final val UpdateUserRequestManifest = classOf[UpdateUserRequest].getName
  final val UpdateUserResponseManifest = classOf[UpdateUserResponse].getName

  final val ListSitesRequestManifest = classOf[ListSitesRequest].getName
  final val ListSitesResponseManifest = classOf[ListSitesResponse].getName
  final val FindSiteRequestManifest = classOf[FindSiteRequest].getName
  final val FindSiteResponseManifest = classOf[FindSiteResponse].getName
  final val CreateSiteRequestManifest = classOf[CreateSiteRequest].getName
  final val CreateSiteResponseManifest = classOf[CreateSiteResponse].getName
  final val UpdateSiteRequestManifest = classOf[UpdateSiteRequest].getName
  final val UpdateSiteResponseManifest = classOf[UpdateSiteResponse].getName
  final val DeleteSiteRequestManifest = classOf[DeleteSiteRequest].getName
  final val DeleteSiteResponseManifest = classOf[DeleteSiteResponse].getName

  final val TraceLogRequestManifest = classOf[TraceLogRequest].getName
  final val TraceLogResponseManifest = classOf[TraceLogResponse].getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case t: TestRequest => t.toByteArray
      case t: TestResponse => t.toByteArray

      case u: FindUserRequest => u.toByteArray
      case u: FindUserResponse => u.toByteArray
      case u: CreateUserRequest => u.toByteArray
      case u: CreateUserResponse => u.toByteArray
      case u: UpdateUserRequest => u.toByteArray
      case u: UpdateUserResponse => u.toByteArray

      case s: ListSitesRequest => s.toByteArray
      case s: ListSitesResponse => s.toByteArray
      case s: FindSiteRequest => s.toByteArray
      case s: FindSiteResponse => s.toByteArray
      case s: CreateSiteRequest => s.toByteArray
      case s: CreateSiteResponse => s.toByteArray
      case s: UpdateSiteRequest => s.toByteArray
      case s: UpdateSiteResponse => s.toByteArray
      case s: DeleteSiteRequest => s.toByteArray
      case s: DeleteSiteResponse => s.toByteArray

      case l: TraceLogRequest => l.toByteArray
      case l: TraceLogResponse => l.toByteArray
    }
  }

}

object MessageSerializer {
  trait RequestMessage
  trait ResponseMessage {
    def responseStatus: Option[ResponseStatus]
  }

  trait UserRequest extends RequestMessage
  trait UserResponse extends ResponseMessage

  trait SiteRequest extends RequestMessage
  trait SiteResponse extends ResponseMessage
}
