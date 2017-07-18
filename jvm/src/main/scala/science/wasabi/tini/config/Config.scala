package science.wasabi.tini.config


import com.typesafe.config.{Config => TypesafeConfig}
import com.typesafe.config.ConfigFactory

import scala.io.Source


object Config {
  // Model for the config file
  case class TiniConfig(envExample: String, discordBotToken: String, killSecret: String, kafka: Kafka)
  case class Kafka(server: String, port: String, topic: String)

  val conf: TiniConfig = {
    val confString: String = Source
      .fromFile("./application.conf")
      .getLines()
      .mkString("\n")

    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._

    loadConfigOrThrow[TiniConfig](hocon)
  }
}
