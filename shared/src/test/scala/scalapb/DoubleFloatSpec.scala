package scalapb

import com.google.protobuf.{CodedOutputStream, CodedInputStream}
import utest._

object DoubleFloatSpec extends TestSuite {
  def byteArray(xs: Int*) = {
    xs.map(_.toByte).toArray
  }

  def withCos[T](f: CodedOutputStream => T): Array[Byte] = {
    val bs = new java.io.ByteArrayOutputStream()
    val cos = CodedOutputStream.newInstance(bs)
    f(cos)
    cos.flush()
    bs.toByteArray
  }

  def validateDouble(d: Double) = {
    val bs: Array[Byte] = withCos(_.writeDoubleNoTag(d))
    val cis = new CodedInputStream(new java.io.ByteArrayInputStream(bs))
    val in = cis.readDouble()
    if (in.isNaN) d.isNaN ==> (true)
    else in ==> (d)
  }

  def validateFloat(f: Float) = {
    val bs: Array[Byte] = withCos(_.writeFloatNoTag(f))
    val cis = new CodedInputStream(new java.io.ByteArrayInputStream(bs))
    val in = cis.readFloat()
    if (in.isNaN) f.isNaN ==> (true)
    else in ==> (f)
  }

  val tests = Tests {
    "writeDoubleNoTag produce correct values" - {
      withCos(_.writeDoubleNoTag(0)) ==> (byteArray(0, 0, 0, 0, 0, 0, 0, 0))
      withCos(_.writeDoubleNoTag(1)) ==> (byteArray(0, 0, 0, 0, 0, 0, -16, 63))
      withCos(_.writeDoubleNoTag(-1)) ==> (byteArray(0, 0, 0, 0, 0, 0, -16,
          -65))
      withCos(_.writeDoubleNoTag(13.1)) ==> (byteArray(51, 51, 51, 51, 51, 51,
          42, 64))
      withCos(_.writeDoubleNoTag(-21.46)) ==> (byteArray(-10, 40, 92, -113, -62,
          117, 53, -64))
      withCos(_.writeDoubleNoTag(Double.NaN)) ==> (byteArray(0, 0, 0, 0, 0, 0,
          -8, 127))
      withCos(_.writeDoubleNoTag(Double.PositiveInfinity)) ==> (byteArray(0, 0,
          0, 0, 0, 0, -16, 127))
      withCos(_.writeDoubleNoTag(Double.NegativeInfinity)) ==> (byteArray(0, 0,
          0, 0, 0, 0, -16, -1))
    }

    "writeFloatNoTag produce correct values" - {
      withCos(_.writeFloatNoTag(0)) ==> (byteArray(0, 0, 0, 0))
      withCos(_.writeFloatNoTag(1)) ==> (byteArray(0, 0, -128, 63))
      withCos(_.writeFloatNoTag(-1)) ==> (byteArray(0, 0, -128, -65))
      withCos(_.writeFloatNoTag(13.1f)) ==> (byteArray(-102, -103, 81, 65))
      withCos(_.writeFloatNoTag(-21.46f)) ==> (byteArray(20, -82, -85, -63))
      withCos(_.writeFloatNoTag(Float.NaN)) ==> (byteArray(0, 0, -64, 127))
      withCos(_.writeFloatNoTag(Float.PositiveInfinity)) ==> (byteArray(
        0,
        0,
        -128,
        127
      ))
      withCos(_.writeFloatNoTag(Float.NegativeInfinity)) ==> (byteArray(
        0,
        0,
        -128,
        -1
      ))
    }

    "reading serialized double return original value" - {
      validateDouble(0)
      validateDouble(1)
      validateDouble(-1)
      validateDouble(13.1)
      validateDouble(-21.46)
      validateDouble(Float.NaN)
      validateDouble(Float.PositiveInfinity)
      validateDouble(Float.NegativeInfinity)
    }

    "reading serialized floats return original value" - {
      validateFloat(0)
      validateFloat(1)
      validateFloat(-1)
      validateFloat(13.1f)
      validateFloat(-21.46f)
      validateFloat(Float.NaN)
      validateFloat(Float.PositiveInfinity)
      validateFloat(Float.NegativeInfinity)
    }
  }
}
