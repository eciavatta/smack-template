package smack.kafka

import java.nio.ByteBuffer

import akka.serialization.Serialization

import scala.util.Try

object ProtobufSerialization {

  private val intSize: Int = 4

  def serializeMessage(elem: AnyRef)(implicit serialization: Serialization): Try[ByteBuffer] = Try {
    val serializer = serialization.findSerializerFor(elem)
    val data = serializer.toBinary(elem)
    val byteBuffer = ByteBuffer.allocate(intSize + data.length)
    byteBuffer.putInt(serializer.identifier)
    byteBuffer.put(data)
  }

  def deserializeMessage(manifest: String, data: ByteBuffer)(implicit serialization: Serialization): Try[AnyRef] =
    serialization.deserialize(data.array().drop(intSize), data.getInt, manifest)

}
