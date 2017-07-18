package science.wasabi.tini.bot


import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import science.wasabi.tini._
import science.wasabi.tini.bot.commands._
import science.wasabi.tini.bot.discord.ingestion.{AkkaCordIngestion, Ingestion}
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.bot.kafka.KafkaStreams
import science.wasabi.tini.config.Config


object BotMain extends App {
  println(Helper.greeting)

  implicit val config = Config.conf
  CommandRegistry.configure(config.bot.commands)

  case class Ping(override val args: String) extends Command(args) {}
  case class NoOp(override val args: String) extends Command(args) {}
  case class UnkownCommand(override val args: String) extends Command(args) {}

  "!ping" match {
    case CommandRegistry(command) => println("testo: " + command)
  }

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
  commandStreamFromKafka.map(command => println("out: " + command))
  .runWith(Sink.ignore)
  .foreach(_ => println("Done Consuming"))

  // TODO: add the reply steam thingy
}

