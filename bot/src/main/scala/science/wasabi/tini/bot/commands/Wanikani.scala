package science.wasabi.tini.bot.commands

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import science.wasabi.tini.bot.discord.ingestion.JdaIngestionActor.{JdaCommands, SendMessage}
import science.wasabi.tini.bot.discord.wrapper.DiscordMessage

import scalaz.{-\/, \/, \/-}


object Wanikani {

  object AddCommand extends Command {override def prefix: String = "!wanikani add "}
  object RemoveCommand extends Command {override def prefix: String = "!wanikani remove"}

  sealed trait Event
  case class Tick() extends Event
  case class FirstTick() extends Event
  case class Kill() extends Event
  case class UpdateQueueState(state: WanikaniQueueState) extends Event

  case class DiscordInfo(channelId: String, userId: String)
  case class WanikaniQueueState(apiKey: String, username: String = "User", lessonAmount: Int = 0, reviewAmount: Int = 0)
  type WanikaniSub = (DiscordInfo, WanikaniQueueState)

  import io.circe.generic.semiauto._
  import org.http4s._
  import org.http4s.circe._
  import io.circe._

  case class UserInfo(username: String)
  case class StudyQueueInfo(lessons_available: Int, reviews_available: Int)
  case class StudyQueueResponse(user_information: UserInfo, requested_information: StudyQueueInfo)

  implicit val userInfoDecoder: Decoder[UserInfo] = deriveDecoder
  implicit val studyQueueInfoDecoder: Decoder[StudyQueueInfo] = deriveDecoder
  implicit val studyQueueResponseDecoder: Decoder[StudyQueueResponse] = deriveDecoder

  implicit val studyQueueResponseJsonDecoder: EntityDecoder[StudyQueueResponse] = jsonOf[StudyQueueResponse]

  private def wanikaniActor(api: ActorRef[JdaCommands], sub: WanikaniSub, httpClient: Client): Behavior[Event] = Actor.immutable {
    def extractQueue(response: StudyQueueResponse): WanikaniQueueState = {
      WanikaniQueueState(
        sub._2.apiKey,
        response.user_information.username,
        response.requested_information.lessons_available,
        response.requested_information.reviews_available
      )
    }

    (ctx, event) => event match {
      case FirstTick() =>
        httpClient.expect[StudyQueueResponse](s"https://www.wanikani.com/api/user/${sub._2.apiKey}/study-queue")
          .unsafePerformAsync {
            case -\/(fail) =>
              println(fail)
              api ! SendMessage(DiscordMessage(
                channel_id = sub._1.channelId,
                content = "Could not contact wanikani! Check that your key is valid and try again later!"
              ))
              ctx.self ! Kill()
            case \/-(success) =>
              val queueState = extractQueue(success)
              api ! SendMessage(DiscordMessage(
                channel_id = sub._1.channelId,
                content = s"Hi <@${sub._1.userId}>! Or should I call you ${queueState.username}? " +
                  s"You have **${queueState.lessonAmount} lessons** and **${queueState.reviewAmount} reviews** waiting! " +
                  s"がんばって!"
              ))
              ctx.self ! UpdateQueueState(queueState)
              ctx.self ! Tick()
          }
        Actor.same
      case Tick() =>
        httpClient.expect[StudyQueueResponse](s"https://www.wanikani.com/api/user/${sub._2.apiKey}/study-queue")
          .unsafePerformAsync {
            case -\/(fail) =>
              println(fail)
              api ! SendMessage(DiscordMessage(channel_id = sub._1.channelId, content = "Could not contact wanikani!"))
            case \/-(success) =>
              val queueState = extractQueue(success)
              val lessonDecrease = sub._2.lessonAmount - queueState.lessonAmount
              val reviewDecrease = sub._2.reviewAmount - queueState.reviewAmount

              if(lessonDecrease > 0 || reviewDecrease > 0) {
                api ! SendMessage(DiscordMessage(
                  channel_id = sub._1.channelId,
                  content = s"<@${sub._1.userId}> just finished **$lessonDecrease lessons** and **$reviewDecrease reviews**! " +
                    s"お疲れ様でした!"
                ))
              }
              ctx.self ! UpdateQueueState(queueState)
          }
        println("TICK")

        import scala.concurrent.duration._
        ctx.schedule(1 minute, ctx.self, Tick())
        // TODO: make sure not to send micro-updates every minute, detect when the learning has stopped.
        Actor.same

      case UpdateQueueState(newState) =>
        wanikaniActor(api, sub.copy(_2 = newState), httpClient)

      case Kill() =>
        Actor.stopped
    }
  }

  def wanikaniCommandActor(api: ActorRef[JdaCommands])(subs: Map[DiscordInfo, ActorRef[Event]]): Behavior[DiscordMessage] = Actor.immutable {
    implicit val httpClient = PooledHttp1Client()
    (ctx, message) => message.content match {
      case AddCommand(args) =>
        // TODO: add the wanikani key via DM, then activate the reports on a channel with a different one.
        api ! SendMessage(message.createReply("Will do! :ok_hand:"))

        val discordInfo = DiscordInfo(message.channel_id, message.author.id)
        val sub = discordInfo -> WanikaniQueueState(args)
        val existingActor = subs.get(discordInfo)

        existingActor.foreach(_ ! Kill())

        val actor = ctx.spawn(wanikaniActor(api, sub, httpClient), "sub-for-" + discordInfo)
        actor ! FirstTick()

        wanikaniCommandActor(api)(subs + (discordInfo -> actor))

      case RemoveCommand(args) =>
        api ! SendMessage(message.createReply("OK! :ok_hand:"))

        val discordInfo = DiscordInfo(message.channel_id, message.author.id)
        val existingActor = subs.get(discordInfo)

        existingActor.foreach(_ ! Kill())

        wanikaniCommandActor(api)(subs - discordInfo)
      case _ => Actor.same
    }
  }
}
