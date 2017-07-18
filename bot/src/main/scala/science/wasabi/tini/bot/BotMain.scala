package science.wasabi.tini.bot


import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import science.wasabi.tini._
import science.wasabi.tini.bot.commands._
import science.wasabi.tini.bot.discord.ingestion.{AkkaCordIngestion, Ingestion}
import science.wasabi.tini.bot.kafka.KafkaStreams
import science.wasabi.tini.config.Config


object BotMain extends App {
  println(Helper.greeting)
  implicit val config = Config.conf

  final case class Ping(override val args: String) extends Command(args)
  final case class NoOp(override val args: String) extends Command(args)

  val ingestion: Ingestion = new AkkaCordIngestion

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system = akka.actor.ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  val streams = new KafkaStreams

  // pipe to kafka
  val commandStream: Source[Command, NotUsed] = ingestion.source.mapConcat[Command](dmsg =>
    CommandRegistry.getCommandsFor(dmsg.content)
  )
  val commandTopicStream = commandStream.map(streams.mapToCommandTopic)
  commandTopicStream
    .runWith(streams.sink)
    .foreach(_ => println("Done Producing"))

  // read from kafka
  val commandStreamFromKafka = streams.sourceFromCommandTopic()
  commandStreamFromKafka.map(command => println("out: " + command))
  .runWith(Sink.ignore)
  .foreach(_ => println("Done Consuming"))

  // TODO: add the reply steam thingy
}

