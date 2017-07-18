package science.wasabi.tini.bot.discord.ingestion

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.sksamuel.avro4s._
import org.apache.avro.generic.GenericRecord
import org.specs2.mutable.Specification

sealed trait Employee {}
final case class RankAndFile(name: String, jobTitle: String) extends Employee
final case class BigBoss(name: String) extends Employee

class AvroSerializationTest extends Specification {
  override def is = s2"""
Serialization
     in avro $e0
"""

  def e0 = true must beTrue

/*
  def e1 = {
    val schema = AvroSchema[Employee]
    val joe = RankAndFile("Joe", "grunt")

    val out = new ByteArrayOutputStream()
    val os = AvroOutputStream.binary[Employee](out)
    os.write(joe)
    os.close()

    val bytes = out.toByteArray

    val in = new ByteArrayInputStream(bytes)
    val is = AvroInputStream.binary[Employee](in)

    for(emp <- is.iterator()) println(emp)

    true must beTrue
  }*/

/*
  def e2 = {
    val out = new ByteArrayOutputStream()
    val os = AvroOutputStream.binary[GenericRecord](out)

    val joe = RankAndFile("Joe", "grunt")

    val format = RecordFormat[Employee]
    // record is of type GenericRecord
    val record = format.to(joe)

    os.write(record)
    os.close()

    val bytes = out.toByteArray

    val in = new ByteArrayInputStream(bytes)
    val is = AvroInputStream.binary[GenericRecord](in)

    for{
      generic <- is.iterator()
      emp = format.from(generic)
    } println(emp)

    true must beTrue
  }*/

  /*
  def e3 = {
    def toBinary[T: SchemaFor : ToRecord](event: T): Array[Byte] = {
      val baos = new ByteArrayOutputStream()
      val output = AvroOutputStream.binary[T](baos)
      output.write(event)
      output.close()
      baos.toByteArray
    }
    def fromBinary[T: SchemaFor : FromRecord](bytes: Array[Byte])= {
      val in = new ByteArrayInputStream(bytes)
      val input = AvroInputStream.binary[T](in)
      input.iterator.toSeq
    }

    val joe = RankAndFile("Joe", "grunt")
    val bytes = toBinary(joe)
    val emps = fromBinary[Employee](bytes)
    for(emp <- emps) println(emp)

    true must beTrue
  }

  */
}
