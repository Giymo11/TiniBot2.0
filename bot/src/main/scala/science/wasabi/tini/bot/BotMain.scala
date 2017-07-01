package science.wasabi.tini.bot

import science.wasabi.tini._
import science.wasabi.tini.bot.discord._
import science.wasabi.tini.config.Config
import science.wasabi.tini.config.Config.TiniConfig

object BotMain extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)

  implicit val config = Config.conf

  import akka.typed._
  import akka.typed.scaladsl.Actor

  case class MessageActorState(name: String, count: Int = 0)
  def printActor(state: MessageActorState): Behavior[DiscordMessage] = Actor.immutable {
    (_, message) =>
      println(s"[${state.count}] ${state.name} sends ${message.content}")
      printActor(state.copy(count = state.count + 1))
  }

  // old way:
  /*-----
  val system1: ActorSystem[DiscordMessage] =
    ActorSystem("messageActorJda", printActor(MessageActorState("JDA")))
  val system2: ActorSystem[DiscordMessage] =
    ActorSystem("messageActorJavacord", printActor(MessageActorState("Javacord")))

  val ingestion1 = new JdaIngestion((message) => system1 ! message)
  val ingestion2 = new JavacordIngestion((message) => system2 ! message)
  //val ingestion3 = new Discord4JIngestion((message) => println("Discord4J says: " + message.content))
  -----*/

  implicit val messageHandler = printActor(MessageActorState("lol"))
  JdaIngestionActor.startup
}

