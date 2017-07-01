package science.wasabi.tini.config


import com.typesafe.config.{Config => TypesafeConfig}
import com.typesafe.config.ConfigFactory

import scala.io.Source


object Config {
  // Model for the config file
  case class TiniConfig(apiKey: String, envExample: String, discordKey: String)

  val conf: TiniConfig = {
    val confString: String = Source
      .fromFile("./application.conf")
      .getLines()
      .mkString("\n")

    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._
    import pureconfig.error.ConfigReaderFailures

    loadConfigOrThrow[TiniConfig](hocon)
  }
}