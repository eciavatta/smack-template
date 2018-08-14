package smack.kafka

import java.nio.ByteBuffer

import akka.serialization.Serialization

import scala.util.Try

object ProtobufSerialization {

  private val intSize: Int = 4

  private[kafka] def serializeMessage(elem: AnyRef)(implicit serialization: Serialization) = Try {
    val serializer = serialization.findSerializerFor(elem)
    val data = serializer.toBinary(elem)
    val byteBuffer = ByteBuffer.allocate(intSize + data.length)
    byteBuffer.putInt(serializer.identifier)
    byteBuffer.put(data)
  }

  private[kafka] def deserializeMessage(manifest: String, data: ByteBuffer)(implicit serialization: Serialization): Try[AnyRef] =
    serialization.deserialize(data.array().drop(intSize), data.getInt, manifest)

}
