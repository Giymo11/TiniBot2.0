package science.wasabi.tini.bot.commands

import org.http4s.client.blaze.PooledHttp1Client
import science.wasabi.tini.bot.replies._

import scalaz.{-\/, \/-}

class Jisho(override val args: String, override val auxData: String) extends Command(args, auxData) {
  def action: Reply = Jisho.action(args, auxData)
}

object Jisho {
  import io.circe._
  import io.circe.generic.semiauto._
  import org.http4s._
  import org.http4s.circe._

  case class Link(text: String, url: String)
  case class Senses(english_definitions: List[String], parts_of_speech: List[String], links: List[Link], tags: List[String],
                    restrictions: List[String], see_also: List[String], antonyms: List[String], source: List[String], info: List[String], sentences: Option[List[String]])
  case class Attribution(jmdict: Boolean, jmnedict: Boolean, dbpedia: Either[Boolean, String])
  case class Japanese(word: Option[String], reading: Option[String])
  case class WordData(is_common: Option[Boolean], tags: List[String], japanese: List[Japanese], senses: List[Senses], attribution: Attribution)
  case class MetaData(status: Int)
  case class Definition(meta: MetaData, data: List[WordData])

  implicit val decodeIntOrString: Decoder[Either[Boolean, String]] = Decoder[Boolean].map(Left(_)).or(Decoder[String].map(Right(_)))
  implicit val SensesDecoder: Decoder[Senses] = deriveDecoder
  implicit val AttributionDecoder: Decoder[Attribution] = deriveDecoder
  implicit val JapaneseDecoder: Decoder[Japanese] = deriveDecoder
  implicit val LinkDecoder: Decoder[Link] = deriveDecoder
  implicit val WordDataDecoder: Decoder[WordData] = deriveDecoder
  implicit val MetaDataDecoder: Decoder[MetaData] = deriveDecoder
  implicit val DefinitionDecoder: Decoder[Definition] = deriveDecoder

  implicit val DefinitionJsonDecoder: EntityDecoder[Definition] = jsonOf[Definition]

  def action(args: String, auxData: String): Reply = {
    println("stato")
    implicit val httpClient = PooledHttp1Client()
    val urlEncoded = java.net.URLEncoder.encode(args, "utf-8")
    val addr = s"http://jisho.org/api/v1/search/words?keyword=$urlEncoded"
    println("before")
    val res = httpClient.expect[Definition](addr).unsafePerformSyncAttempt
    res match {
      case -\/(fail) =>
        println(fail)
        NoReply()
      case \/-(success) =>
        if (success.data.isEmpty) {
          EmbedReply(
            auxData,
            (255, 0, 0),
            "Jisho",
            args,
            List(
              ("Error", "Definition not found", true)
            )
          )
        }
        else {
          val jap = success.data.head.japanese.head
          val eng = success.data.head.senses.head.english_definitions.head
          val repStr = s"${jap.word.getOrElse("")} [${jap.reading.getOrElse("")}] -> $eng"
          EmbedReply(
            auxData,
            (138, 188, 131),
            "Jisho",
            args,
            List(
              ("Word", jap.word.getOrElse("None"), true),
              ("Reading", jap.reading.getOrElse("None"), true),
              ("Definition", eng, true)
            )
          )
        }
    }
  }
}
