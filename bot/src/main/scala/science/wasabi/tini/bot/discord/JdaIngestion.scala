package science.wasabi.tini.bot.discord


import net.dv8tion.jda.core._
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import science.wasabi.tini.config.Config.TiniConfig


class JdaIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {

  import DiscordWrapperConverter._

  val jda: JDA =
    new JDABuilder(AccountType.BOT)
      .setToken(config.discordKey)
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
