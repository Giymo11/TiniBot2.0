package science.wasabi.tini.bot.discord.ingestion

import net.katsstuff.akkacord.APIMessage
import net.katsstuff.akkacord.data._
import org.specs2.mutable.Specification
import prickle.{CompositePickler, Pickle, PicklerPair, Unpickle}



sealed trait Fruit
case class Apple(isJuicy: Boolean) extends Fruit
case class Lemon(sourness: Double) extends Fruit


class PrickleSerialization extends Specification {

  override def is = s2"""
Serialization
     in avro $e0
"""

  def e0 = true must beTrue

  implicit val fruitPickler: PicklerPair[Fruit] = CompositePickler[Fruit].concreteType[Apple].concreteType[Lemon]

  /*
  def e1 = {
    val apple = Apple(true)
    val pickledApples = Pickle.intoString[Fruit](apple)

    val fruit = Unpickle[Fruit].fromString(pickledApples)
    fruit.get must_==(apple)
  }

  def e2 = {
    implicit val ApiPickler: PicklerPair[APIMessage] = CompositePickler[APIMessage].concreteType[APIMessage.MessageCreate]
    val msg = APIMessage.MessageCreate(
      Message(MessageId(Snowflake("")),ChannelId(Snowflake("")),null,"helloWorld",null,None,false,false,Seq(),Seq(),Seq(),Seq(),Seq(),None,false,None),
      CacheSnapshot(null,null,null,null,null,null,null,null),
      CacheSnapshot(null,null,null,null,null,null,null,null))

    val string = Pickle.intoString[APIMessage](msg)
    val apiMsg = Unpickle[APIMessage].fromString(string)

    println(apiMsg)

    true must beTrue
  }
  */

}
