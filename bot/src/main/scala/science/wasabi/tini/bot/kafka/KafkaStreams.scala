package science.wasabi.tini.bot.kafka


import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import science.wasabi.tini.BinaryHelper
import science.wasabi.tini.bot.commands.Command
import science.wasabi.tini.bot.replies._
import science.wasabi.tini.config.Config.TiniConfig

import scala.concurrent.Future

class KafkaStreams(implicit config: TiniConfig, system: ActorSystem) {
  val kafkaServer = s"${config.kafka.server}:${config.kafka.port}"

  // Producer Sink
  val sink: Sink[ProducerRecord[Array[Byte], Array[Byte]], Future[Done]] = {
    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
      .withBootstrapServers(kafkaServer)
    Producer.plainSink(producerSettings)
  }

  def toReplyTopic(reply: Reply): ProducerRecord[Array[Byte], Array[Byte]] = {
    println("reply in: " + reply)
    new ProducerRecord[Array[Byte], Array[Byte]](config.kafka.replytopic, BinaryHelper.toBinary(reply))
  }

  def toCommandTopic(command: Command): ProducerRecord[Array[Byte], Array[Byte]] = {
    println("in: " + command)
    new ProducerRecord[Array[Byte], Array[Byte]](config.kafka.commandtopic, BinaryHelper.toBinary(command))
  }

  case class KafkaParseError(override val args: String, override val auxData: String) extends Command(args, auxData) {
    def action: Reply = NoReply()
  }

  case class KafkaReplyParseError() extends Reply

  def sourceFromCommandTopic(): Source[Command, Consumer.Control] = {
    // kafka consumer
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(kafkaServer)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val partition = 0
    val offset = 0L // starts from beginnning; TODO: save offset somewhere
    val subscription = Subscriptions.topics(config.kafka.commandtopic)

    Consumer.plainSource(consumerSettings, subscription).map(record => {
      BinaryHelper.fromBinary[Command](record.value).fold(
        throwable => KafkaParseError(throwable.getMessage, ""),
        command => command
      )
    })
  }

  def sourceFromReplyTopic(): Source[Reply, Consumer.Control] = {
    // kafka consumer
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(kafkaServer)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val partition = 0
    val offset = 0L // starts from beginnning; TODO: save offset somewhere
    val subscription = Subscriptions.topics(config.kafka.replytopic)

    Consumer.plainSource(consumerSettings, subscription).map(record => {
      BinaryHelper.fromBinary[Reply](record.value).fold(
        throwable => KafkaReplyParseError(),
        reply => reply
      )
    })
  }
}
