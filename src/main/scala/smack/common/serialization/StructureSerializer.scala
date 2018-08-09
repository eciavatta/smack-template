package smack.common.serialization

import akka.serialization.SerializerWithStringManifest
import smack.models.structures._

class StructureSerializer extends SerializerWithStringManifest {

  private val serializableId = 9001
  override def identifier: Int = serializableId

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case UserManifest => User.parseFrom(bytes)
      case DateManifest => Date.parseFrom(bytes)
    }

  override def manifest(o: AnyRef): String = o.getClass.getName
  final val DateManifest = classOf[Date].getName
  final val UserManifest = classOf[User].getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case u: User => u.toByteArray
      case d: Date => d.toByteArray
    }
  }

}
