package science.wasabi.tini.bot.commands


trait Command {
    def prefix: String
    def unapply(arg: String): Option[String] =  if (arg startsWith prefix) Some(arg.drop(prefix.length).trim) else None
}

