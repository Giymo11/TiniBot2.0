package science.wasabi.tini.bot

import science.wasabi.tini._
import science.wasabi.tini.config.Config

object HelloWorld extends App {
  println(Helper.greeting)

  println("Key = " + Config.conf.apiKey)
  println("Path = " + Config.conf.envExample)
}
