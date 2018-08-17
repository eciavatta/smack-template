package smack.common.serialization

import akka.serialization.SerializerWithStringManifest
import smack.models.structures._

class StructureSerializer extends SerializerWithStringManifest {

  private val serializableId = 9001
  override def identifier: Int = serializableId

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case DateManifest => Date.parseFrom(bytes)
      case UserManifest => User.parseFrom(bytes)
      case SiteManifest => Site.parseFrom(bytes)
      case ResponseStatusManifest => ResponseStatus.parseFrom(bytes)
    }

  override def manifest(o: AnyRef): String = o.getClass.getName
  final val DateManifest = classOf[Date].getName
  final val UserManifest = classOf[User].getName
  final val SiteManifest = classOf[Site].getName
  final val ResponseStatusManifest = classOf[ResponseStatus].getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case d: Date => d.toByteArray
      case u: User => u.toByteArray
      case s: Site => s.toByteArray
      case rs: ResponseStatus => rs.toByteArray
    }
  }

}
