package science.wasabi.tini.bot.discord.ingestion


import akka.NotUsed
import akka.actor._
import akka.event.EventStream
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import net.katsstuff.akkacord.data.{ChannelId, EmbedField, OutgoingEmbed, Snowflake}
import net.katsstuff.akkacord.http.rest.Requests
import net.katsstuff.akkacord.http.rest.Requests.CreateMessageData
import net.katsstuff.akkacord.{APIMessage, DiscordClient, DiscordClientSettings, Request}
import science.wasabi.tini.bot.commands.{Command, CommandRegistry}
import science.wasabi.tini.bot.discord.wrapper.DiscordWrapperConverter
import science.wasabi.tini.bot.replies._
import science.wasabi.tini.config.Config.TiniConfig

import scala.collection.immutable.Iterable


class AkkaCordApi(shutdownCallback: () => Unit)(implicit config: TiniConfig) extends Api {
  implicit val system: ActorSystem = ActorSystem("AkkaCord")
  implicit val materializer = ActorMaterializer()

  private val eventStream = new EventStream(system)

  val client: ActorRef = DiscordClientSettings(token = config.discordBotToken, system = system, eventStream = eventStream).connect

  private val (ref, publisher) = Source.actorRef[APIMessage.MessageCreate](32, OverflowStrategy.dropHead)
    .map(in => DiscordWrapperConverter.AkkaCordConverter.convertMessage(in.message))
    .toMat(Sink.asPublisher(true))(Keep.both)
    .run()

  eventStream.subscribe(ref, classOf[APIMessage.MessageCreate])

  val source: Source[Command, NotUsed] = {
    def string2command(string: String, auxData: String): Iterable[Command] = CommandRegistry.getCommandsFor(string, auxData)

    Source.fromPublisher(publisher).mapConcat[Command](dmsg => string2command(dmsg.content, dmsg.channel_id))
  }

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
        fields = rep.fields.map(f =>
          EmbedField(f._1, f._2, Some(f._3))
        )
      ))
    )))
    case rep: ShutdownReply =>
      shutdown()
      shutdownCallback()
    case _ => None
  }
}
