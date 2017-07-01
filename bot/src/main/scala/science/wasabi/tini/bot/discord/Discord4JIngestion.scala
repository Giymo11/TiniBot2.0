package science.wasabi.tini.bot.discord


import science.wasabi.tini.config.Config.TiniConfig

import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent


class Discord4JIngestion(listener: DiscordMessage => Unit)(implicit config: TiniConfig) {

  import DiscordWrapperConverter._

  val discordClient: IDiscordClient =
    new ClientBuilder()
        .withToken(config.discordKey)
        .login()

  discordClient.getDispatcher.registerListener(
    (event: ReadyEvent) => println("READY IN D4J")
  )
  discordClient.getDispatcher.registerListener(
    (event: MessageReceivedEvent) => listener(event.getMessage)
  )
}