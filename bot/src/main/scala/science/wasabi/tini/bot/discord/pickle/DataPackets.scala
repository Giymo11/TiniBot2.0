/**
  * Created by Stefl1504 on 02.07.2017.
  */
package science.wasabi.tini.bot.discord.pickle

import java.security.MessageDigest
import java.util.Base64
import java.math.BigInteger

object DataPackets {
  val base64 = Base64.getEncoder
  val digester = MessageDigest.getInstance("MD5")

  sealed trait Data
  case class Caster(name: String, occupation: Long, id: Long) extends Data{
    override def toString() = {
      name + occupation + id
    }
  }

  case class GrandPickle(data: Seq[Data], timestamp: Long, signature: String = ""){
    override def toString() = {
      data.map(caster => caster.toString).mkString("") + timestamp
    }
  }

  def pickleCaster(name: String, occupation: Long, id: Long) = {
    val toPickle = GrandPickle( Seq(Caster(name, occupation, id)), System.currentTimeMillis())
    generateSignature(toPickle)
  }

  def pickleCasters(casters: Map[String, Seq[Long]]) = {
    val prePickle = casters.keys.map(name => {
      Caster(name, casters(name)(0), casters(name)(1))
    }).toSeq
    val toPickle = GrandPickle(prePickle, System.currentTimeMillis())
    generateSignature(toPickle)
  }

  def generateSignature(pickle: GrandPickle) = {
    val appliedString = pickle.toString
    val splitIndex = appliedString.length()/2 + appliedString.length() % 2
    val hashString = appliedString.take(splitIndex) + "superSecretSaltString" + appliedString.drop(splitIndex)
    val hash = new BigInteger(1, digester.digest(hashString.getBytes("UTF-8"))).toString(16)
    pickle.copy(signature = new String(base64.encode(hash.getBytes("UTF-8"))))
  }
}