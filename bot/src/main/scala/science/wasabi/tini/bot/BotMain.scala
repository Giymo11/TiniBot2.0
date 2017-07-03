package science.wasabi.tini.bot


import science.wasabi.tini._
import science.wasabi.tini.bot.commands.{Command, Wanikani}
import science.wasabi.tini.bot.commands.webstream.Webstream
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor._
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.config.Config


object BotMain extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)

  implicit val config = Config.conf

  import akka.typed._
  import akka.typed.scaladsl.Actor

  object PingCommand extends Command {override def prefix: String = "!ping"}
  object KillCommand extends Command {override def prefix: String = "!kill " + config.killSecret}

  def respondingActor(api: ActorRef[JdaCommands]): Behavior[DiscordMessage] = Actor.immutable {
    (ctx, message) => message.content match {
      case PingCommand(args) =>
        api ! SendMessage(message.createReply("PONG!"))
        Actor.same
      case KillCommand(args) =>
        api ! Shutdown()
        Actor.same
      case text =>
        println("lol: " + text)
        Actor.same
    }
  }


  val handlers: Seq[ActorRef[JdaCommands] => Behavior[DiscordMessage]] = Seq(
    respondingActor(_),
    Wanikani.wanikaniCommandActor(_)(Map()),
    Webstream.webstreamActor(_)
  )

  val ingestionActor = JdaIngestionActor.startup(handlers)
}

