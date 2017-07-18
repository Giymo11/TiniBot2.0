package science.wasabi.tini.bot


import akka.Done
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import science.wasabi.tini._
import science.wasabi.tini.bot.discord.ingestion.AkkaCordIngestion
import science.wasabi.tini.config.Config

import scala.concurrent.Future


object BotMain extends App {
  println(Helper.greeting)
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

  val doneProducer = ingestion.source
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
  val subscription = Subscriptions.topics(config.kafka.topic)
  val doneConsumer = Consumer.plainSource(consumerSettings, subscription)
    .mapAsync(1)(record => {
      println("out: " + record.value())
      Future.successful(Done)
    })
    .runWith(Sink.ignore)
    .foreach(_ => println("Done Consuming"))
}

