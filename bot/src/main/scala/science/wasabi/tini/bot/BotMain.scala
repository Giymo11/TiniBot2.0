package science.wasabi.tini.bot


import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import science.wasabi.tini._
import science.wasabi.tini.bot.discord.ingestion.AkkaCordIngestion
import science.wasabi.tini.bot.kafka.KafkaStreams
import science.wasabi.tini.config.Config

import scala.concurrent.Future


object BotMain extends App {
  println(Helper.greeting)
  implicit val config = Config.conf

  val ingestion = new AkkaCordIngestion

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system = akka.actor.ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  val streams = new KafkaStreams

  // pipe to kafka
  val discordMessageStream = ingestion.source
  val commandStream = ingestion.source.map(_.toString) // TODO: actually map to commands
  val commandTopicStream = commandStream.map(streams.mapToCommandTopic)
  commandTopicStream
    .runWith(streams.commandSink)
    .foreach(_ => println("Done Producing"))

  // read from kafka
  val commandStreamFromKafka = streams.sourceFromCommandTopic()
  commandStreamFromKafka.mapAsync(1)(record => { // TODO: actually add the logic
    println("out: " + record.value())
    Future.successful(Done)
  })
  .runWith(Sink.ignore)
  .foreach(_ => println("Done Consuming"))

}

