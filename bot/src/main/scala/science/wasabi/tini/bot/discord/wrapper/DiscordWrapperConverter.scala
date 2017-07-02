package science.wasabi.tini.bot.discord.wrapper

import net.katsstuff.akkacord.APIMessage

object DiscordWrapperConverter {
  object JdaConverter {
    import net.dv8tion.jda.core.entities.{Message => JdaMessage}
    import net.dv8tion.jda.core.{MessageBuilder => JdaMessageBuilder}
    implicit def convertMessage(message: JdaMessage): DiscordMessage = DiscordMessage(
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
    implicit def toJda(message: DiscordMessage): JdaMessage = new JdaMessageBuilder()
      .setTTS(message.tts)
      .append(message.content)
      .build()
  }

  object Discord4JConverter {
    import sx.blah.discord.handle.obj.{IMessage => Discord4JMessage}
    implicit def convertMessage(message: Discord4JMessage): DiscordMessage = DiscordMessage(
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
  }

  object JavacordConverter {
    import de.btobastian.javacord.entities.message.{Message => JavacordMessage}
    implicit def convertMessage(message: JavacordMessage): DiscordMessage = DiscordMessage(
      message.getId,
      message.getChannelReceiver.getId,
      User(message.getAuthor.getId),
      message.getContent,
      message.getCreationDate.toString,
      None,
      message.isTts,
      message.isMentioningEveryone,
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

  object AkkaCordConverter {
    import net.katsstuff.akkacord.data.{Message => AkkaCordMessage}
    implicit def convertMessage(message: AkkaCordMessage): DiscordMessage = DiscordMessage(
      message.id.toString,
      message.channelId.toString,
      User(""), // TODO: proper handling of this stuff
      message.content,
      message.timestamp.toString,
      message.editedTimestamp.map(_.toString),
      message.tts,
      message.mentionEveryone,
      pinned = message.pinned
    )
  }
}
