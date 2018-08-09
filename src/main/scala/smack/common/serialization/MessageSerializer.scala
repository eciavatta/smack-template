package smack.common.serialization

import akka.serialization.SerializerWithStringManifest
import smack.models.messages._

class MessageSerializer extends SerializerWithStringManifest {

  private val serializableId = 9002
  override def identifier: Int = serializableId

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case TestRequestManifest => TestRequest.parseFrom(bytes)
      case TestResponseManifest => TestResponse.parseFrom(bytes)

      case GetUsersRequestManifest => GetUsersRequest.parseFrom(bytes)
      case GetUsersResponseManifest => GetUsersResponse.parseFrom(bytes)
      case GetUserRequestManifest => GetUserRequest.parseFrom(bytes)
      case GetUserResponseManifest => GetUserResponse.parseFrom(bytes)
      case CreateUserRequestManifest => CreateUserRequest.parseFrom(bytes)
      case CreateUserResponseManifest => CreateUserResponse.parseFrom(bytes)
      case DeleteUserRequestManifest => DeleteUserRequest.parseFrom(bytes)
      case DeleteUserResponseManifest => DeleteUserResponse.parseFrom(bytes)
    }

  override def manifest(o: AnyRef): String = o.getClass.getName
  final val TestRequestManifest = classOf[TestRequest].getName
  final val TestResponseManifest = classOf[TestResponse].getName

  final val GetUsersRequestManifest = classOf[GetUsersRequest].getName
  final val GetUsersResponseManifest = classOf[GetUsersResponse].getName
  final val GetUserRequestManifest = classOf[GetUserRequest].getName
  final val GetUserResponseManifest = classOf[GetUserResponse].getName
  final val CreateUserRequestManifest = classOf[CreateUserRequest].getName
  final val CreateUserResponseManifest = classOf[CreateUserResponse].getName
  final val DeleteUserRequestManifest = classOf[DeleteUserRequest].getName
  final val DeleteUserResponseManifest = classOf[DeleteUserResponse].getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case t: TestRequest => t.toByteArray
      case t: TestResponse => t.toByteArray

      case g: GetUsersRequest => g.toByteArray
      case g: GetUsersResponse => g.toByteArray
      case g: GetUserRequest => g.toByteArray
      case g: GetUserResponse => g.toByteArray
      case c: CreateUserRequest => c.toByteArray
      case c: CreateUserResponse => c.toByteArray
      case c: DeleteUserRequest => c.toByteArray
      case c: DeleteUserResponse => c.toByteArray
    }
  }

}

object MessageSerializer {
  trait RequestMessage
  trait ResponseMessage {
    def statusCode: Int
  }

  trait UserRequest extends RequestMessage
  trait UserResponse extends ResponseMessage
}
