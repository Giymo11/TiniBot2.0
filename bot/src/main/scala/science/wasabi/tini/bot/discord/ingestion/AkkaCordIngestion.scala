package science.wasabi.tini.bot.discord.ingestion


import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.EventStream

import net.katsstuff.akkacord.{APIMessage, DiscordClientSettings}

import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.config.Config.TiniConfig

import science.wasabi.tini.bot.discord.wrapper.DiscordWrapperConverter.AkkaCordConverter._


class AkkaCordIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {
  implicit val system: ActorSystem = ActorSystem("AkkaCord")

  val eventStream = new EventStream(system)

  val client: ActorRef = DiscordClientSettings(token = config.discordBotToken, system = system, eventStream = eventStream).connect

  val props = Props(classOf[Commands], listener)
  eventStream.subscribe(
    system.actorOf(props, "CommandActor"),
    classOf[APIMessage.MessageCreate]
  )
}

class Commands(listener: DiscordMessage => Unit) extends Actor with ActorLogging {
  override def receive: Receive = {
    case APIMessage.MessageCreate(message, cacheSnapshot, prevSnapshot) =>
      listener(message)
  }
}