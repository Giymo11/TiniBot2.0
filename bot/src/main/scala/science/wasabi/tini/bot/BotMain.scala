package science.wasabi.tini.bot


import scala.collection.immutable.Iterable

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import science.wasabi.tini._
import science.wasabi.tini.bot.commands._
import science.wasabi.tini.bot.discord.ingestion.{AkkaCordIngestion, Ingestion}
import science.wasabi.tini.bot.kafka.KafkaStreams
import science.wasabi.tini.config.Config
import science.wasabi.tini.bot.replies._

object BotMain extends App {
  println(Helper.greeting)

  implicit val config = Config.conf
  CommandRegistry.configure(config.bot.commands)

  class Ping(override val args: String, override val auxData: String) extends Command(args, auxData) {
    def action: Reply = {
      SimpleReply(auxData, "PONG")
    }
  }
  class NoOp(override val args: String, override val auxData: String) extends Command(args, auxData) {
    def action: Reply = NoReply()
  }

  val ingestion: Ingestion = new AkkaCordIngestion

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system = akka.actor.ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  val kafka = new KafkaStreams

  // pipe to kafka
  def string2command(string: String, auxData: String): Iterable[Command] = CommandRegistry.getCommandsFor(string, auxData)
  val commandStream: Source[Command, NotUsed] = ingestion.source.mapConcat[Command](dmsg => string2command(dmsg.content, dmsg.channel_id))
  val commandTopicStream = commandStream.map(kafka.toCommandTopic)
  commandTopicStream
    .runWith(kafka.sink)
    .foreach(_ => println("Done Producing"))

  // read from kafka, do stuff and send back
  val commandStreamFromKafka = kafka.sourceFromCommandTopic()
  commandStreamFromKafka
    .map(command => {
      command.action
    }
    )
    .map(kafka.toReplyTopic)
    .runWith(kafka.sink)

  //read replies

  val replyStreamFromKafka = kafka.sourceFromReplyTopic()
  replyStreamFromKafka
    .map(reply => ingestion.handleReply(reply))
    .runWith(Sink.ignore)

  // TODO: add the reply steam thingy
}

