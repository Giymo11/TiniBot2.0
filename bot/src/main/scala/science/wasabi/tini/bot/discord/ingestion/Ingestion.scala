package science.wasabi.tini.bot.discord.ingestion


import akka.NotUsed
import akka.stream.scaladsl.Source

import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.bot.replies.Reply

trait Ingestion {
  val source: Source[DiscordMessage, NotUsed]
  def handleReply(reply: Reply)
}
