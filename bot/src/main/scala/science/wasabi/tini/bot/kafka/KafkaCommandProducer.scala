package science.wasabi.tini.bot.kafka

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.Properties

import com.sksamuel.avro4s.AvroOutputStream
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import science.wasabi.tini.bot.protocol.Command
import science.wasabi.tini.config.Config.TiniConfig

import scala.concurrent.Future


/**
  * @author Raphael
  * @version 17.07.2017
  */
class KafkaCommandProducer(topic: String)(implicit config: TiniConfig) {

  private val props = new Properties()
  lazy val producer = new KafkaProducer[String, Array[Byte]](props)

  props.put("client.id", InetAddress.getLocalHost.getHostName)
  props.put("bootstrap.servers", config.kafka.server + ":" + config.kafka.port)
  props.put("acks", "all")
  props.put("retries", "3")
  props.put("key.serializer", classOf[StringSerializer])
  props.put("value.serializer", classOf[ByteArraySerializer])
  //props.put("producer.type", "async")
  props.put("linger.ms", "0")
  //props.put("timeout.ms", "250")
  props.put("group.id", s"tini-$topic")
  props.put("schema.registry.url", config.kafka.shemaURI)

  def produce(command: Command): Future[RecordMetadata] = {

    implicit def convertToBinary(command: Command): Array[Byte] = {
      val out = new ByteArrayOutputStream()
      val output = AvroOutputStream.binary[Command](out)
      output.write(command)
      output.close()

      val bytes = out.toByteArray
      out.close()

      bytes
    }

    val record = new ProducerRecord[String, Array[Byte]](topic, command)

    /* Make async to avid hanging of the actor */
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.blocking
    Future {
      blocking {
        try {
          val f = producer.send(record)
          producer.flush()
          f.get()
        } catch {
          case ex: Exception =>
            ex.printStackTrace()
            null
        }
      }
    }
  }



}
