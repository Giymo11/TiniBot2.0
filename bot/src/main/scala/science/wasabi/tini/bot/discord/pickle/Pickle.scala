package science.wasabi.tini.bot.discord.pickle

import science.wasabi.tini.bot.discord.wrapper.DiscordMessage
import science.wasabi.tini.config.Config
import scala.util.matching.Regex
import upickle.default._

object Pickle{

  implicit val config = Config.conf
  //RAW (!cast\s+(-(ana|obs|coc)\s+@\w+#\d*\s*|@\w+#\d+\s*){1,4}(\w+\s+vs\s+\w+)?)
  val regCommandExp = "!cast\\s+(-(ana|obs|coc)\\s+<@\\d+>*\\s*|<@\\d+>\\s*){1,4}(\\w+\\s+vs\\s+\\w+)?".r

  //RAW (-(cas|ana|obs|coc))?\s*@\w+#\d+
  val regCasterExp = "(-(cas|ana|obs|coc))?\\s*<@\\d+>".r

  val regRoleExp = "-(cas|ana|obs|coc)".r

  val roleLibrary = Map("-cas" -> 2, "-ana" -> 1, "-obs" -> 3,"-coc" -> 4)

  /**
    *
    * @param message a discord message to parse
    * @param sendToServer send to specified server
    * @param sendToTwitch not yet Implemented, default= false
    * @return
    */
  def parse(message: DiscordMessage) = {
    val casterMap = regCasterExp.findAllMatchIn(message.content).map(caster => {
      regRoleExp.findFirstMatchIn(caster.toString) match {
        case Some(x) =>
          x.after.toString.trim().drop(2).takeWhile(c => c != '>') -> Seq(roleLibrary(x.toString), 150490123478009777l)
        case None =>
          caster.toString.trim().drop(2).takeWhile(c => c != '>') -> Seq(2, 150490123478009777l)
      }
    }).toMap
    val pickledData = DataPackets.pickleCasters(casterMap)
    "The Data was pickled as follows: " + write(pickledData)
  }

  def isPickleable(string : String) = {
    regCommandExp.findFirstMatchIn(string) match {
      case Some(x) => true
      case None => false
    }
  }
}
