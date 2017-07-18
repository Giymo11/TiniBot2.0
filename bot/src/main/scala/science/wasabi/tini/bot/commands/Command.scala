package science.wasabi.tini.bot.commands

import science.wasabi.tini.bot.BotMain.{NoOp, Ping}


object CommandRegistry {

  val commandRegistry: Map[String, Class[_ <: Command]] = Map(
    "!ping" -> classOf[Ping],
    "" -> classOf[NoOp]
  )

  def getCommandsFor(args: String): scala.collection.immutable.Iterable[Command] = commandRegistry
    .filter { case (string, _) => args.startsWith(string) }
    .map { case (string, clazz) => clazz
      .getConstructor(classOf[String])
      .newInstance(args.drop(string.length).trim)
    }

  def unapply(args: String): Option[Command] = commandRegistry
    .find { case (string, _) => args.startsWith(string) }
    .map { case (string, clazz) => clazz
      .getConstructor(classOf[String])
      .newInstance(args.drop(string.length).trim)
    }
}

abstract class Command(argsIn: String) extends Serializable {
  val args: String = argsIn
}

