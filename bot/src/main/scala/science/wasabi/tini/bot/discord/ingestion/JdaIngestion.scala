package science.wasabi.tini.bot.discord.ingestion


import java.util.Base64

import akka.typed.scaladsl.Actor
import net.dv8tion.jda.core._
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.config.Config.TiniConfig
import science.wasabi.tini.bot.discord.wrapper.DiscordWrapperConverter.JdaConverter._

/**
  * A way to add listeners as lambdas
  * @param listener
  * @param config
  */
class JdaIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {

  val jda: JDA =
    new JDABuilder(AccountType.BOT)
      .setToken(config.discordBotToken)
      .addEventListener(new ListenerAdapter {
        override def onReady(event: ReadyEvent): Unit = println("READY")
        override def onGuildMessageReceived(event: GuildMessageReceivedEvent): Unit = {
          listener(event.getMessage)
        }
        override def onPrivateMessageReceived(event: PrivateMessageReceivedEvent): Unit = {
          listener(event.getMessage)
        }
      })
      .buildAsync()
}


object JdaIngestionActor {
  import akka.typed._

  trait JdaCommands
  case class Ready(jda: JDA) extends JdaCommands
  case class Shutdown() extends JdaCommands
  case class SendMessage(message: DiscordMessage) extends JdaCommands

  def alternatively[T](one: Option[T], alt: () => Option[T]) = if(one.isEmpty) alt() else one

  def supervisor(jda: JDA): Behavior[JdaCommands] =
    Actor.immutable{ (ctx, message) =>
      message match {
        case event: Shutdown =>
          println("Shutting down")
          jda.shutdown(true)
          Actor.stopped
        case SendMessage(msgToSend) =>
          val channel = alternatively(
            Option(jda.getTextChannelById(msgToSend.channel_id)).filter(_.canTalk),
            () => Option(jda.getPrivateChannelById(msgToSend.channel_id))
          )
          channel.foreach { channel =>
            println("tryna send a msg: " + msgToSend)
            channel.sendMessage(msgToSend).queue()
          }
          Actor.same
      }
    }

  def starting(handlers: Seq[ActorRef[JdaCommands] => Behavior[DiscordMessage]]): Behavior[JdaCommands] =
    Actor.immutable { (ctx, message) =>
      message match {
        case event: Ready =>
          println("JDA is ready")
          val encoder = Base64.getEncoder
          for(handlerBehavior <- handlers) {
            val handler = ctx.spawn(
              handlerBehavior(ctx.self),
              "messageHandler" + encoder.encode(handlerBehavior.toString().getBytes)
            )
            event.jda.addEventListener(new ListenerAdapter {
              override def onGuildMessageReceived(event: GuildMessageReceivedEvent): Unit = handler ! event.getMessage
              override def onPrivateMessageReceived(event: PrivateMessageReceivedEvent): Unit = handler ! event.getMessage
            })
          }

          supervisor(event.jda)
      }
    }

  /**
    * A way to spawn an actor system to listen for messages.
    * @param config
    */
  def startup(handlers: Seq[ActorRef[JdaCommands] => Behavior[DiscordMessage]])(implicit config: TiniConfig) = {
    val system: ActorSystem[JdaCommands] = ActorSystem("jdaActor", starting(handlers))

    val jda = new JDABuilder(AccountType.BOT)
      .setToken(config.discordBotToken)
      .addEventListener(new ListenerAdapter {
        override def onReady(event: ReadyEvent): Unit = system ! Ready(event.getJDA)
      })
      .buildAsync()

    system
  }
}