package science.wasabi.tini.bot

import science.wasabi.tini._
import science.wasabi.tini.bot.discord.JdaIngestionActor._
import science.wasabi.tini.bot.discord._
import science.wasabi.tini.config.Config

object BotMain extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)

  implicit val config = Config.conf

  import akka.typed._
  import akka.typed.scaladsl.Actor

  def respondingActor(api: ActorRef[Commands]): Behavior[DiscordMessage] = Actor.immutable {
    (ctx, message) => message match {
      case ping if ping.content == "!ping" =>
        api ! SendMessage(DiscordMessage(channel_id = ping.channel_id, content = "PONG!"))
        Actor.same
      case kill if kill.content == "!kill " + config.killSecret =>
        api ! Shutdown()
        Actor.same
      case text =>
        println("lol: " + text.content)
        Actor.same
    }
  }

  implicit val messageHandler = respondingActor _

  val ingestionActor = JdaIngestionActor.startup
}

