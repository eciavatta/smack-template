package smack.serialization

import akka.serialization.SerializerWithStringManifest
import smack.models.messages._

class ModelSerializer extends SerializerWithStringManifest {

  private val serializableId = 9001
  override def identifier: Int = serializableId

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case GetUsersRequestManifest => GetUsersRequest.parseFrom(bytes)
      case GetUsersResponseManifest => GetUsersResponse.parseFrom(bytes)
    }

  override def manifest(o: AnyRef): String = o.getClass.getName
  final val GetUsersRequestManifest = classOf[GetUsersRequest].getName
  final val GetUsersResponseManifest = classOf[GetUsersResponse].getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case g: GetUsersRequest => g.toByteArray
      case g: GetUsersResponse => g.toByteArray
    }
  }

}

object ModelSerializer {
  trait RequestMessage
  trait ResponseMessage
}
