package science.wasabi.tini.bot.discord


import com.google.common.util.concurrent.FutureCallback
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.listener.message.MessageCreateListener
import de.btobastian.javacord.{DiscordAPI, Javacord}
import science.wasabi.tini.config.Config.TiniConfig


class JavacordIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {

  import DiscordWrapperConverter._

  val javacord: DiscordAPI =
    Javacord
      .getApi(config.discordKey, true)

  javacord.connect(new FutureCallback[DiscordAPI] {
    override def onFailure(t: Throwable): Unit = println("Shit happened in Javacord")
    override def onSuccess(result: DiscordAPI): Unit = {
      result.registerListener(new MessageCreateListener {
        override def onMessageCreate(discordAPI: DiscordAPI, message: Message): Unit = listener(message)
      })
    }
  })
}

/* package science.wasabi.tini.bot.discord

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
*/