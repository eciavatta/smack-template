package smack.kafka

import java.nio.ByteBuffer

import akka.NotUsed
import akka.actor.ActorRef
import akka.kafka.ProducerMessage
import akka.kafka.ProducerMessage.MultiResultPart
import akka.serialization.Serialization
import akka.stream.scaladsl.Flow
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import smack.kafka.KafkaProducer.{EmptyResult, KafkaMessage, MultiKafkaResult, SingleKafkaResult}

import scala.util.Try

object ProtobufSerialization {

  private val intSize: Int = 4

  def deserialize(implicit serialization: Serialization): Flow[ProducerMessage.Results[String, ByteBuffer, ActorRef], KafkaProducer.KafkaResult, NotUsed] =
    Flow[ProducerMessage.Results[String, ByteBuffer, ActorRef]].map {
      case ProducerMessage.Result(metadata, message) =>
        createSingleKafkaResult(metadata, message.record, message.passThrough)
      case ProducerMessage.MultiResult(parts, passThrough) =>
        MultiKafkaResult(parts.map {
          case MultiResultPart(metadata, record) =>
            createSingleKafkaResult(metadata, record, passThrough)
        }.toList, passThrough)
      case ProducerMessage.PassThroughResult(passThrough) =>
        EmptyResult(passThrough)
    }

  private[kafka] def serializeMessage(elem: AnyRef)(implicit serialization: Serialization) = Try {
    val serializer = serialization.findSerializerFor(elem)
    val data = serializer.toBinary(elem)
    val byteBuffer = ByteBuffer.allocate(intSize + data.length)
    byteBuffer.putInt(serializer.identifier)
    byteBuffer.put(data)
  }

  private[kafka] def deserializeMessage(manifest: String, data: ByteBuffer)(implicit serialization: Serialization): Try[AnyRef] =
    serialization.deserialize(data.array().drop(intSize), data.getInt, manifest)

  private def createSingleKafkaResult(metadata: RecordMetadata, record: ProducerRecord[String, ByteBuffer], passThrough: ActorRef)
                                     (implicit serialization: Serialization): SingleKafkaResult =
    SingleKafkaResult(metadata.topic,
      metadata.partition,
      if (metadata.hasOffset) Some(metadata.offset) else None,
      if (metadata.hasTimestamp) Some(metadata.timestamp) else None,
      record.key,
      deserializeMessage(record.key, record.value),
      passThrough)

}
