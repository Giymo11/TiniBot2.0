package science.wasabi.tini.bot


import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, KillSwitches}
import science.wasabi.tini._
import science.wasabi.tini.bot.commands._
import science.wasabi.tini.bot.discord.ingestion.{AkkaCordApi, Api}
import science.wasabi.tini.bot.kafka.KafkaStreams
import science.wasabi.tini.bot.replies._
import science.wasabi.tini.config.Config

object BotMain extends App {
  println(Helper.greeting)

  implicit val config = Config.conf
  CommandRegistry.configure(config.bot.commands)

  class Ping(override val args: String, override val auxData: String) extends Command(args, auxData) {
    def action: Reply = SimpleReply(auxData, "PONG")
  }
  class NoOp(override val args: String, override val auxData: String) extends Command(args, auxData) {
    def action: Reply = NoReply()
  }

  class Shutdown(override val args: String, override val auxData: String) extends Command(args, auxData) {
    def action: Reply = {
      if(args equals config.killSecret)
        ShutdownReply()
      else
        NoReply()
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val kafkaSystem = akka.actor.ActorSystem("kafka")
  implicit val materializer = ActorMaterializer()

  val sharedKillSwitch = KillSwitches.shared("shutdown")

  val shutdownCallback: () => Unit = () => {
    sharedKillSwitch.shutdown()
    kafkaSystem.terminate()
    ()
  }

  val api: Api = new AkkaCordApi(shutdownCallback)
  val kafka = new KafkaStreams

  // pipe to kafka
  val commandTopicStream = api.source.map(kafka.toCommandTopic)
  commandTopicStream
    .via(sharedKillSwitch.flow)
    .runWith(kafka.sink)
    .foreach(_ => println("Done Producing"))

  // read from kafka, do stuff and send back
  val commandStreamFromKafka = kafka.sourceFromCommandTopic()
  commandStreamFromKafka
    .map(command => {
      command.action
    })
    .map(kafka.toReplyTopic)
    .via(sharedKillSwitch.flow)
    .runWith(kafka.sink)

  //read replies
  val replyStreamFromKafka = kafka.sourceFromReplyTopic()
  replyStreamFromKafka
    .map(reply => api.handleReply(reply)) // maybe we should separate the APIs
    .via(sharedKillSwitch.flow)
    .runWith(Sink.ignore)
}

