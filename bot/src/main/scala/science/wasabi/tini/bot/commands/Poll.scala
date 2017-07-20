package science.wasabi.tini.bot.commands

import science.wasabi.tini.bot.replies.{EditReply, Reply, SimpleReply}
import science.wasabi.tini.bot.util.AuxData

import scala.collection.mutable

case class PollData(question: String, answers: Map[Int, (String, Int)], totalVotes: Int)

object Poll {
  var createdPoll: Option[PollData] = None
  val assignedPolls: scala.collection.mutable.Map[String, PollData] = new mutable.HashMap[String, PollData]

  def strToPoll(arg: String): PollData = {
    val split = arg.split("\"")
    val question = split(1)
    val answers = split.slice(2, split.length).zipWithIndex.collect {
      case (a, i) if i % 2 != 0 => a
    }
    PollData(question, answers.map(a => a -> 0).zipWithIndex.map(a => (a._2, a._1)).toMap, 0)
  }

  private def answerString(index: Int, ans: String, votes: Int, totalVotes: Int): String = {
    val perc = if(totalVotes != 0) ((100/totalVotes)*votes)/10 else 0
    s"""
       |$index: $ans
       |${"="*perc}
     """.stripMargin
  }

  def pollToStr(arg: PollData): String =
      s"""
        |${arg.question}
        |${arg.answers.toArray.map(ans => answerString(ans._1, ans._2._1, ans._2._2, arg.totalVotes)).mkString("")}
      """.stripMargin

  def assignPoll(messageId: String): Unit = createdPoll match {
    case Some(poll) => {
      assignedPolls.put(messageId, poll)
      createdPoll = None
    }
    case None => ()
  }

  def newVote(poll: PollData, index: Int): PollData = PollData(
    poll.question,
    poll.answers.toArray.map { case (i, vote) =>
      if(i == index) (i, (vote._1, vote._2 + 1)) else (i, vote)
    }.toMap,
    poll.totalVotes+1
  )
}


class PollCreate(override val args: String, override val auxData: AuxData) extends Command(args, auxData) {
  def action: Reply = {
    val poll = Poll.strToPoll(args)
    Poll.createdPoll = Some(poll)
    SimpleReply(auxData.channelId, Poll.pollToStr(poll))
  }
}

class PollVote(override val args: String, override val auxData: AuxData) extends Command(args, auxData) {
  def action: Reply = {
    val index = args.toInt
    val (id, poll) = Poll.assignedPolls.head
    val updatedPoll = Poll.newVote(poll, index)
    Poll.assignedPolls.put(id, updatedPoll)
    EditReply(auxData.channelId, id, Poll.pollToStr(updatedPoll))
  }
}

