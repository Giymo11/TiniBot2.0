package science.wasabi.tini.bot.commands

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor.{JdaCommands, SendMessage}
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage


object Wanikani {

  object AddCommand extends Command {override def prefix: String = "!wanikani add "}
  object RemoveCommand extends Command {override def prefix: String = "!wanikani remove"}

  trait Event
  case class Tick() extends Event
  case class Kill() extends Event

  case class DiscordInfo(channelId: String, userId: String)
  case class WanikaniQueueState(apiKey: String, lastLessonAmount: Int = 0, lastReviewAmount: Int = 0)
  type WanikaniSub = (DiscordInfo, WanikaniQueueState)


  case class user_information()
  case class StudyQueueResponse()

  private def wanikaniActor(api: ActorRef[JdaCommands], sub: WanikaniSub)(implicit httpClient: Client): Behavior[Event] = Actor.immutable {
    (ctx, event) => event match {
      case Tick() =>
        () //TODO
        println("TICK")

        import scala.concurrent.duration._
        ctx.schedule(1 minute, ctx.self, Tick())
        Actor.same
      case Kill() =>
        Actor.stopped
    }
  }

  def wanikaniCommandActor(api: ActorRef[JdaCommands])(subs: Map[DiscordInfo, ActorRef[Event]]): Behavior[DiscordMessage] = Actor.immutable {
    implicit val httpClient = PooledHttp1Client()
    (ctx, message) => message.content match {
      case AddCommand(args) =>
        api ! SendMessage(message.createReply("Will do! :ok_hand:"))

        val discordInfo = DiscordInfo(message.channel_id, message.author.id)
        val sub = discordInfo -> WanikaniQueueState(args)
        val existingActor = subs.get(discordInfo)

        if(existingActor.isEmpty) {
          val actor = ctx.spawn(wanikaniActor(api, sub), "sub-for-" + discordInfo)
          actor ! Tick()
          wanikaniCommandActor(api)(subs + (discordInfo -> actor))
        } else Actor.same

      case RemoveCommand(args) =>
        api ! SendMessage(message.createReply("OK! :ok_hand:"))

        val discordInfo = DiscordInfo(message.channel_id, message.author.id)
        val existingActor = subs.get(discordInfo)

        existingActor.foreach(_ ! Kill())
        Actor.same
      case _ => Actor.same
    }
  }
}
