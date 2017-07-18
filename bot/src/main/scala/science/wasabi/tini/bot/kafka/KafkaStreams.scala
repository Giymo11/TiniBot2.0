package science.wasabi.tini.bot.kafka

import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import science.wasabi.tini.bot.BotMain.{Command, system}
import science.wasabi.tini.config.Config.TiniConfig


class KafkaStreams(implicit config: TiniConfig) {
  val kafkaServer = s"${config.kafka.server}:${config.kafka.port}"

  // kafka producer
  private val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(kafkaServer)

  val sink = Producer.plainSink(producerSettings)

  def mapToCommandTopic(command: Command): ProducerRecord[Array[Byte], String] = {
    println("in: " + command)
    new ProducerRecord[Array[Byte], String](config.kafka.topic, command.toString)
  }

  def sourceFromCommandTopic() = {
    // kafka consumer
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(kafkaServer)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val partition = 0
    val offset = 0L // starts from beginnning; TODO: save offset somewhere
    val subscription = Subscriptions.topics(config.kafka.topic)
    Consumer.plainSource(consumerSettings, subscription)
  }
}
