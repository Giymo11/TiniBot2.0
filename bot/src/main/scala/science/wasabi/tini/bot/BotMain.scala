package science.wasabi.tini.bot


import akka.{Done, NotUsed}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import science.wasabi.tini._
import science.wasabi.tini.bot.discord.ingestion.{AkkaCordIngestion, Ingestion}
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.bot.kafka.KafkaStreams
import science.wasabi.tini.config.Config

import scala.concurrent.Future


object BotMain extends App {
  println(Helper.greeting)
  implicit val config = Config.conf

  object CommandRegistry {

    val commandRegistry: Map[String, Class[_ <: Command]] = Map(
      "!ping" -> classOf[Ping],
      "" -> classOf[NoOp]
    )

    def getCommandsFor(args: String): scala.collection.immutable.Iterable[Command] = commandRegistry
      .filter(p => args.startsWith(p._1))
      .map(f => f._2.getConstructor(classOf[String]).newInstance(args.drop(f._1.length + 1).trim))

    def unapply(args: String): Option[Command] = commandRegistry
      .find { case (string, _) => args.startsWith(string) }
      .map { case (string, clazz) => clazz
        .getConstructor(classOf[String])
        .newInstance(args.drop(string.length + 1).trim)
      }
  }

  trait Command {}
  case class Ping(args: String) extends Command {}
  case class NoOp(args: String) extends Command {}

  println(CommandRegistry.unapply("!ping"))

  val ingestion: Ingestion = new AkkaCordIngestion

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system = akka.actor.ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  val streams = new KafkaStreams

  // pipe to kafka
  val discordMessageStream: Source[DiscordMessage, NotUsed] = ingestion.source
  val commandStream: Source[Command, NotUsed] = discordMessageStream.mapConcat[Command](dmsg =>
    CommandRegistry.getCommandsFor(dmsg.content)
  ) // TODO: actually map to commands
  val commandTopicStream = commandStream.map(streams.mapToCommandTopic)
  commandTopicStream
    .runWith(streams.sink)
    .foreach(_ => println("Done Producing"))

  // read from kafka
  val commandStreamFromKafka = streams.sourceFromCommandTopic()
  commandStreamFromKafka.mapAsync(1)(record => {
    println("out: " + record.value()) // TODO: actually add the logic
    Future.successful(Done)
  })
  .runWith(Sink.ignore)
  .foreach(_ => println("Done Consuming"))

  // TODO: add the reply steam thingy
}

