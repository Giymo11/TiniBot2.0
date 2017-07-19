package science.wasabi.tini.bot.replies

trait Reply
case class NoReply() extends Reply
case class SimpleReply(channelId: String, content: String) extends Reply
case class EmbedReply(channelId: String, color: (Int, Int, Int), title: String, description: String, fields: List[(String, String, Boolean)]) extends Reply
case class ShutdownReply() extends Reply