package science.wasabi.tini.bot.commands

import science.wasabi.tini.bot.BotMain.{NoOp, Ping}

import scala.util.{Failure, Success, Try}


object CommandRegistry {

  private var commandRegistry: Map[String, Class[_ <: Command]] = Map(
    "!ping" -> classOf[Ping],
    "" -> classOf[NoOp]
  )

  def configure(commands: Map[String, String]): Unit = {

    case class CommandNotFoundError(__not_used: String) extends Command(argsIn = __not_used) with Serializable
    def convertToClass(name: String): Class[Command] = Try(Class.forName(name)) match {
      case Success(commandClazz) => commandClazz.asInstanceOf[Class[Command]]
      case Failure(exception) => exception.printStackTrace(); classOf[CommandNotFoundError].asInstanceOf[Class[Command]]
    }

    CommandRegistry.commandRegistry = commands.map {
      case (name: String, clazz: String) => (name, convertToClass(clazz))
    }

    println(s"Commands are: ${CommandRegistry.commandRegistry}")
  }

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

abstract class Command(argsIn: String) extends Serializable {
  val args: String = argsIn
}

