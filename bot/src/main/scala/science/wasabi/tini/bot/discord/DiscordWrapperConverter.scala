package science.wasabi.tini.bot.discord


object DiscordWrapperConverter {

  import sx.blah.discord.handle.obj.IMessage
  implicit def convertMessage(message: IMessage): DiscordMessage = DiscordMessage(
    message.getStringID,
    message.getChannel.getStringID,
    User(message.getAuthor.getStringID),
    message.getContent,
    message.getCreationDate.toString,
    Option(message.getEditedTimestamp.orElseGet(null)).map(_.toString),
    tts = false,
    mention_everyone = message.mentionsEveryone,
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    None,
    pinned = message.isPinned,
    None
  )

  import net.dv8tion.jda.core.entities.Message
  implicit def convertMessage(message: Message): DiscordMessage = DiscordMessage(
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
