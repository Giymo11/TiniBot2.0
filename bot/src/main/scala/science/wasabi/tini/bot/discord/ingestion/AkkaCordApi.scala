package science.wasabi.tini.bot.discord.ingestion


import akka.NotUsed
import akka.actor._
import akka.event.EventStream
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import net.katsstuff.akkacord.data._
import net.katsstuff.akkacord.http.rest.Requests
import net.katsstuff.akkacord.http.rest.Requests.{CreateMessageData, EditMessageData}
import net.katsstuff.akkacord.{APIMessage, DiscordClient, DiscordClientSettings, Request}
import science.wasabi.tini.bot.commands.{Command, CommandRegistry}
import science.wasabi.tini.bot.discord.wrapper.{DiscordMessage, DiscordWrapperConverter, User}
import science.wasabi.tini.bot.replies._
import science.wasabi.tini.bot.util.AuxData
import science.wasabi.tini.config.Config.TiniConfig

import scala.collection.immutable.Iterable


class AkkaCordApi(shutdownCallback: () => Unit)(implicit config: TiniConfig) extends Api {
  implicit val system: ActorSystem = ActorSystem("AkkaCord")
  implicit val materializer = ActorMaterializer()

  private val eventStream = new EventStream(system)

  val client: ActorRef = DiscordClientSettings(token = config.discordBotToken, system = system, eventStream = eventStream).connect

  private val (ref, publisher) = Source.actorRef[APIMessage](32, OverflowStrategy.dropHead)
    .map {
      case msg: APIMessage.MessageCreate => DiscordWrapperConverter.AkkaCordConverter.convertMessage(msg.message)
      case msg => DiscordMessage(author = User("None"))
    }
      .toMat(Sink.asPublisher(true))(Keep.both)
        .run()

      eventStream.subscribe(ref, classOf[APIMessage])

      val source: Source[Command, NotUsed] = {
        def string2command(string: String, auxData: AuxData): Iterable[Command] = CommandRegistry.getCommandsFor(string, auxData)

        Source.fromPublisher(publisher).mapConcat[Command](dmsg => string2command(dmsg.content, AuxData(dmsg.channel_id, dmsg.id, dmsg.author.id)))      }

      def shutdown() = {
        client ! DiscordClient.ShutdownClient
        system.terminate()
      }

      def handleReply(reply: Reply) = reply match {
        case rep: SimpleReply => client ! Request(Requests.CreateMessage(ChannelId(Snowflake(rep.channelId)), CreateMessageData(rep.content, None, tts = false, None, None)))
        case rep: EmbedReply => client ! Request(Requests.CreateMessage(ChannelId(Snowflake(rep.channelId)), CreateMessageData("", None, tts = false, None,
          Some(OutgoingEmbed(
            title = Some(rep.title),
            description = Some(rep.description),
            color = Some(rep.color._3 + rep.color._2 * 16 + rep.color._1 * 16 * 16),
            fields = rep.fields.map { case (title, description, inline) =>
              EmbedField(title, description, Some(inline))
            }
          ))
        )))
        case rep: EditReply => client ! Request(Requests.EditMessage(ChannelId(Snowflake(rep.channelId)), MessageId(Snowflake(rep.messageId)), EditMessageData(Some(rep.content), None)))
        case rep: ShutdownReply =>
          shutdown()
          shutdownCallback()
        case _ => None
      }
    }
