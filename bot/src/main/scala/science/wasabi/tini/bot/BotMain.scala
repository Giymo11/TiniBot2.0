package science.wasabi.tini.bot


import science.wasabi.tini._
import science.wasabi.tini.bot.commands.{Command, Wanikani}
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor._
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.bot.kafka.{KafkaCommandProducer, KafkaCommandConsumer}
import science.wasabi.tini.config.Config

import scala.util.{Failure, Success, Try}


object BotMain extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)

  implicit val config = Config.conf

  import com.sksamuel.avro4s.AvroSchema
  println(AvroSchema[protocol.Command])



  import akka.typed._
  import akka.typed.scaladsl.Actor

  object PingCommand extends Command {override def prefix: String = "!ping"}
  object KillCommand extends Command {override def prefix: String = "!kill " + config.killSecret}

  def respondingActor(api: ActorRef[JdaCommands]): Behavior[DiscordMessage] = Actor.immutable {
    import science.wasabi.tini.bot.protocol.KafkaProtocolConverter._

    (ctx, message) => message.content match {
      case PingCommand(args) =>
        kafkaProducer.produce(message)

        api ! SendMessage(message.createReply("PONG!"))
        Actor.same
      case KillCommand(args) =>
        kafkaProducer.produce(message)

        api ! Shutdown()
        Actor.same
      case text =>
        println("lol: " + text)
        Actor.same
    }


  }

  val handlers: Seq[ActorRef[JdaCommands] => Behavior[DiscordMessage]] = Seq(
    respondingActor(_),
    Wanikani.wanikaniCommandActor(_)(Map())
  )

  val ingestionActor = Try {
    JdaIngestionActor.startup(handlers)
  } match {
    case Success(actor) => actor
    case Failure(_) =>
      System.err.println("Could not load JDA, shutting down Bot ...")
      System.exit(1)
  }


  val kafkaProducer = new KafkaCommandProducer(config.kafka.topic)
  val kafkaConsumer = new KafkaCommandConsumer(config.kafka.topic)
  val consumerPollThread = new Thread(kafkaConsumer)
  consumerPollThread.setDaemon(true)
  consumerPollThread.start()

}

