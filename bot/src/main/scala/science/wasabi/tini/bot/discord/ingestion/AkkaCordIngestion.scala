package science.wasabi.tini.bot.discord.ingestion


import akka.NotUsed
import akka.actor._
import akka.event.EventStream
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}

import net.katsstuff.akkacord.{APIMessage, DiscordClientSettings, Request}
import net.katsstuff.akkacord.http.rest.Requests
import net.katsstuff.akkacord.http.rest.Requests.CreateMessageData
import net.katsstuff.akkacord.data.{ChannelId, Snowflake}

import science.wasabi.tini.bot.discord.wrapper.{DiscordMessage, DiscordWrapperConverter}
import science.wasabi.tini.config.Config.TiniConfig
import science.wasabi.tini.bot.replies._

class AkkaCordIngestion(implicit config: TiniConfig) extends Ingestion {
  implicit val system: ActorSystem = ActorSystem("AkkaCord")
  implicit val materializer = ActorMaterializer()

  private val eventStream = new EventStream(system)

  val client: ActorRef = DiscordClientSettings(token = config.discordBotToken, system = system, eventStream = eventStream).connect

  private val (ref, publisher) = Source.actorRef[APIMessage.MessageCreate](32, OverflowStrategy.dropHead)
    .map(in => DiscordWrapperConverter.AkkaCordConverter.convertMessage(in.message))
    .toMat(Sink.asPublisher(true))(Keep.both)
    .run()


  eventStream.subscribe(ref, classOf[APIMessage.MessageCreate])

  val source: Source[DiscordMessage, NotUsed] = Source.fromPublisher(publisher)


  def shutdown() = system.terminate()

  def handleReply(reply: Reply) = reply match {
    case rep: SimpleReply => client ! Request(Requests.CreateMessage(ChannelId(Snowflake(rep.channelId)), CreateMessageData(rep.content, None, tts = false, None, None)))
    case _ => None
  }
}
