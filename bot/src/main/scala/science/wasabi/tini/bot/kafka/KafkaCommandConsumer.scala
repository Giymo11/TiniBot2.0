package science.wasabi.tini.bot.kafka

import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.util
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

import com.sksamuel.avro4s.AvroInputStream
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import science.wasabi.tini.bot.protocol.Command
import science.wasabi.tini.config.Config.TiniConfig

/**
  * @author Raphael
  * @version 17.07.2017
  */
class KafkaCommandConsumer(topic: String)(implicit config: TiniConfig) extends Runnable {

  private val props = new Properties()
  lazy val consumer = new KafkaConsumer[String, Array[Byte]](props)

  props.put("client.id", InetAddress.getLocalHost.getHostName)
  props.put("bootstrap.servers", config.kafka.server+":"+config.kafka.port)
  props.put("group.id", s"tini-$topic")
  //props.put("consumer.timeout.ms", "10000")
  props.put("key.deserializer", classOf[StringDeserializer])
  props.put("value.deserializer", classOf[ByteArrayDeserializer])
  props.put("schema.registry.url", config.kafka.shemaURI)

  val running = new AtomicBoolean(true)

  override def run(): Unit = {
    consumer.subscribe(util.Arrays.asList(topic))
    println("start")

    try {
      while (running.get()) {

        val records = consumer.poll(500)
        records.forEach { record =>
          val in = new ByteArrayInputStream(record.value)
          val input = AvroInputStream.binary[Command](in)
          val result = input.iterator.toSeq

          println("Got Command: " + result)
        }

        consumer.commitSync()
      }
    } catch {
      case ex: Exception => ex.printStackTrace()
    }

    println("exit ...")
  }

  def kill(): Unit = {
    running.set(false)
    consumer.wakeup()
  }
}
