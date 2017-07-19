package science.wasabi.tini.bot.discord.ingestion


import akka.NotUsed
import akka.stream.scaladsl.Source

import science.wasabi.tini.bot.commands.Command
import science.wasabi.tini.bot.replies.Reply


trait Api {
  val source: Source[Command, NotUsed]
  def handleReply(reply: Reply): Unit
}
