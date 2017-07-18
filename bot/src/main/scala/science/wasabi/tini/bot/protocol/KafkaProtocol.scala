package science.wasabi.tini.bot.protocol

/**
  * @author Raphael
  * @version 17.07.2017
  */


case class User(id: String, name: String)
case class Channel(id: String)
case class Message(user: User, channel: Channel, rawContent: String)

case class Command(user: User, channel: Channel, command: String)

object KafkaProtocolConverter {

  import science.wasabi.tini.bot.commands.{Command => BotCommand}
  import science.wasabi.tini.bot.discord.wrapper.{DiscordMessage, User => DiscordUser}


  implicit def discordUserToKafka(discordUser: DiscordUser): User =
    User(discordUser.id, discordUser.name)

  implicit def discordMessageToChannel(discordMessage: DiscordMessage): Channel =
    Channel(discordMessage.channel_id)

  implicit def discordMessageToKafka(discordMessage: DiscordMessage): Message =
    Message(discordMessage.author, discordMessage, discordMessage.content)

  implicit def discordMessageToKafkaCommand(discordMessage: DiscordMessage): Command =
    Command(discordMessage.author, discordMessage, discordMessage.content) // Todo: only do command stuff

}