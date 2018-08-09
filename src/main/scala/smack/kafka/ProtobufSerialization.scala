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

object ProtobufSerialization {

  private val intSize: Int = 4

  def serialize(implicit serialization: Serialization): Flow[KafkaMessage, ProducerMessage.Message[String, ByteBuffer, ActorRef], NotUsed] =
    Flow[KafkaMessage].map { m =>
      val serializer = serialization.findSerializerFor(m.value)
      val data = serializer.toBinary(m.value)
      val byteBuffer = ByteBuffer.allocate(intSize + data.length)
      byteBuffer.putInt(serializer.identifier)
      byteBuffer.put(data)
      ProducerMessage.Message(
        new ProducerRecord(m.topic, m.partition, m.key, byteBuffer),
        m.sender)
    }

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

  private def createSingleKafkaResult(metadata: RecordMetadata, record: ProducerRecord[String, ByteBuffer], passThrough: ActorRef)
                                     (implicit serialization: Serialization): SingleKafkaResult = {
    SingleKafkaResult(metadata.topic,
      metadata.partition,
      if (metadata.hasOffset) Some(metadata.offset) else None,
      if (metadata.hasTimestamp) Some(metadata.timestamp) else None,
      record.key,
      serialization.deserialize(record.value.array().drop(intSize), record.value.getInt, record.key),
      passThrough)
  }

}
