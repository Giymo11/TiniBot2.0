package science.wasabi.tini.bot.discord


trait DiscordObject {
  type Snowflake = String
  type Timestamp = String

  type User = DiscordObject
  type Role = DiscordObject
  type Attachment = DiscordObject
  type Embed = DiscordObject
  type Reaction = DiscordObject
}


case class User(id: String) extends DiscordObject


case class DiscordMessage(id: String,
                          channel_id: String,
                          author: User,
                          content: String,
                          timestamp: String,
                          edited_timestamp: Option[String],
                          tts: Boolean,
                          mention_everyone: Boolean,
                          mentions: Seq[DiscordObject],
                          mention_roles: Seq[DiscordObject],
                          attachments: Seq[DiscordObject],
                          embeds: Seq[DiscordObject],
                          reactions: Seq[DiscordObject],
                          nonce: Option[String],
                          pinned: Boolean,
                          webhook_id: Option[String]
                  ) extends DiscordObject {}

