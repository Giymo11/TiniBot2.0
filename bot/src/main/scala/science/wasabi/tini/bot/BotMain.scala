package science.wasabi.tini.bot

import science.wasabi.tini._
import science.wasabi.tini.bot.discord.JdaIngestion
import science.wasabi.tini.config.Config

object BotMain extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)

  implicit val config = Config.conf

  val ingestion1 = new JdaIngestion(println)

}
