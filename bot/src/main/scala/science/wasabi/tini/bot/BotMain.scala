package science.wasabi.tini.bot


import akka.Done
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import science.wasabi.tini._
import science.wasabi.tini.bot.commands.{Command, Wanikani}
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor._
import science.wasabi.tini.bot.discord.ingestion.{AkkaCordIngestion, JdaIngestionActor}
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.config.Config

import scala.concurrent.Future


object BotMain extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)

  implicit val config = Config.conf

  val ingestion = new AkkaCordIngestion

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system = akka.actor.ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  import org.apache.kafka.common.serialization._

  val kafkaServer = s"${config.kafka.server}:${config.kafka.port}"

  // kafka producer
  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(kafkaServer)

  val doneProducer = Source(1 to 5)
    .map(_.toString)
    .map(elem => {
      println("in: " + elem)
      new ProducerRecord[Array[Byte], String](config.kafka.topic, elem)
    })
    .runWith(Producer.plainSink(producerSettings))
    .foreach(_ => println("Done Producing"))

  // kafka consumer
  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaServer)
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val partition = 0
  val offset = 0L // starts from beginnning; TODO: save offset somewhere
  val subscription = Subscriptions.assignmentWithOffset(
    new TopicPartition(config.kafka.topic, partition) -> offset
  )
  val doneConsumer = Consumer.plainSource(consumerSettings, subscription)
    .mapAsync(1)(record => {
      println("out: " + record.value())
      Future.successful(Done)
    })
    .runWith(Sink.ignore)
    .foreach(_ => println("Done Consuming"))
}

