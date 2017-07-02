package science.wasabi.tini.bot.discord.ingestion


import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.config.Config.TiniConfig


class Discord4JIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {

  import science.wasabi.tini.bot.discord.wrapper.DiscordWrapperConverter.Discord4JConverter._

  val discordClient: IDiscordClient =
    new ClientBuilder()
        .withToken(config.discordBotToken)
        .login()

  discordClient.getDispatcher.registerListener(
    (event: ReadyEvent) => println("READY IN D4J")
  )
  discordClient.getDispatcher.registerListener(
    (event: MessageReceivedEvent) => listener(event.getMessage)
  )
}