package science.wasabi.tini.bot.discord


import net.dv8tion.jda.core._
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import science.wasabi.tini.config.Config.TiniConfig

class JdaIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {
  val jda: JDA =
    new JDABuilder(AccountType.BOT)
      .setToken(config.discordKey)
      .addEventListener(new ListenerAdapter {
        override def onReady(event: ReadyEvent): Unit = println("READY")
        override def onGuildMessageReceived(event: GuildMessageReceivedEvent): Unit = {
          listener(JdaIngestion.convert(event.getMessage))
        }
        override def onPrivateMessageReceived(event: PrivateMessageReceivedEvent): Unit = {
          listener(JdaIngestion.convert(event.getMessage))
        }
      })
      .buildAsync()

}

object JdaIngestion {
  def convert(message: Message): DiscordMessage = {
    DiscordMessage(
      message.getId,
      message.getChannel.getId,
      User(message.getAuthor.getId),
      message.getRawContent,
      message.getCreationTime.toString,
      Option(message.getEditedTime).map(_.toString),
      message.isTTS,
      message.mentionsEveryone,
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      Seq(),
      None,
      message.isPinned,
      None
    )
  }
}
