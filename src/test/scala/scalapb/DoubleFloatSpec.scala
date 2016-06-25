import org.scalatest._

import com.google.protobuf.{ CodedOutputStream, CodedInputStream }

class DoubleFloatSpec extends FlatSpec with MustMatchers {
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
    if (in.isNaN) d.isNaN must be (true)
    else in must be (d)
  }

  def validateFloat(f: Float) = {
    val bs: Array[Byte] = withCos(_.writeFloatNoTag(f))
    val cis = new CodedInputStream(new java.io.ByteArrayInputStream(bs))
    val in = cis.readFloat()
    if (in.isNaN) f.isNaN must be (true)
    else in must be (f)
  }


  "writeDoubleNoTag" should "produce correct values" in {
    withCos(_.writeDoubleNoTag(0)) must be (byteArray(0, 0, 0, 0, 0, 0, 0, 0))
    withCos(_.writeDoubleNoTag(1)) must be (byteArray(0, 0, 0, 0, 0, 0, -16, 63))
    withCos(_.writeDoubleNoTag(-1)) must be (byteArray(0, 0, 0, 0, 0, 0, -16, -65))
    withCos(_.writeDoubleNoTag(13.1)) must be (byteArray(51, 51, 51, 51, 51, 51, 42, 64))
    withCos(_.writeDoubleNoTag(-21.46)) must be (byteArray(-10, 40, 92, -113, -62, 117, 53, -64))
    withCos(_.writeDoubleNoTag(Double.NaN)) must be (byteArray(0, 0, 0, 0, 0, 0, -8, 127))
    withCos(_.writeDoubleNoTag(Double.PositiveInfinity)) must be (byteArray(0, 0, 0, 0, 0, 0, -16, 127))
    withCos(_.writeDoubleNoTag(Double.NegativeInfinity)) must be (byteArray(0, 0, 0, 0, 0, 0, -16, -1))
  }

  "writeFloatNoTag" should "produce correct values" in {
    withCos(_.writeFloatNoTag(0)) must be (byteArray(0, 0, 0, 0))
    withCos(_.writeFloatNoTag(1)) must be (byteArray(0, 0, -128, 63))
    withCos(_.writeFloatNoTag(-1)) must be (byteArray(0, 0, -128, -65))
    withCos(_.writeFloatNoTag(13.1f)) must be (byteArray(-102, -103, 81, 65))
    withCos(_.writeFloatNoTag(-21.46f)) must be (byteArray(20, -82, -85, -63))
    withCos(_.writeFloatNoTag(Float.NaN)) must be (byteArray(0, 0, -64, 127))
    withCos(_.writeFloatNoTag(Float.PositiveInfinity)) must be (byteArray(0, 0, -128, 127))
    withCos(_.writeFloatNoTag(Float.NegativeInfinity)) must be (byteArray(0, 0, -128, -1))
  }

  "reading serialized double" should "return original value" in {
    validateDouble(0)
    validateDouble(1)
    validateDouble(-1)
    validateDouble(13.1)
    validateDouble(-21.46)
    validateDouble(Float.NaN)
    validateDouble(Float.PositiveInfinity)
    validateDouble(Float.NegativeInfinity)
  }

  "reading serialized floats" should "return original value" in {
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
