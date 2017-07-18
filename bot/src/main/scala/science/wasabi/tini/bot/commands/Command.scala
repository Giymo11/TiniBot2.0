package science.wasabi.tini.bot.commands

import science.wasabi.tini.bot.BotMain.{NoOp, Ping}


object CommandRegistry {

  val commandRegistry: Map[String, Class[_ <: Command]] = Map(
    "!ping" -> classOf[Ping],
    "" -> classOf[NoOp]
  )

  def getCommandsFor(args: String): scala.collection.immutable.Iterable[Command] = commandRegistry
    .filter(p => args.startsWith(p._1))
    .map(f => f._2.getConstructor(classOf[String]).newInstance(args.drop(f._1.length + 1).trim))

  def unapply(args: String): Option[Command] = commandRegistry
    .find { case (string, _) => args.startsWith(string) }
    .map { case (string, clazz) => clazz
      .getConstructor(classOf[String])
      .newInstance(args.drop(string.length + 1).trim)
    }
}

abstract class Command(argsIn: String) {
  val args: String = argsIn
}

