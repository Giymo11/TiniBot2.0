package science.wasabi.tini

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import scala.util.Try

object BinaryHelper {
  def toBinary[T](command: T): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val os = new ObjectOutputStream(out)
    os.writeObject(command)
    os.close()
    out.toByteArray
  }

  def fromBinary[T](data: Array[Byte]): Try[T] = Try {
    val in = new ByteArrayInputStream(data)
    val is = new ObjectInputStream(in)
    val obj = is.readObject()
    is.close()
    in.close()
    obj.asInstanceOf[T]
  }
}
