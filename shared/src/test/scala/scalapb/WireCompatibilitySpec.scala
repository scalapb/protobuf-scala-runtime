package scalapb

import com.google.protobuf.{ByteString, CodedInputStream, CodedOutputStream}
import java.io.ByteArrayOutputStream
import utest._

object WireCompatibilitySpec extends TestSuite {
  def hex(s: String): Array[Byte] =
    s.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  def write(out: CodedOutputStream, kind: String, value: Long): Unit =
    kind match {
      case "int32"    => out.writeInt32NoTag(value.toInt)
      case "uint32"   => out.writeUInt32NoTag(value.toInt)
      case "sint32"   => out.writeSInt32NoTag(value.toInt)
      case "fixed32"  => out.writeFixed32NoTag(value.toInt)
      case "sfixed32" => out.writeSFixed32NoTag(value.toInt)
      case "enum"     => out.writeEnumNoTag(value.toInt)
      case "int64"    => out.writeInt64NoTag(value)
      case "uint64"   => out.writeUInt64NoTag(value)
      case "sint64"   => out.writeSInt64NoTag(value)
      case "fixed64"  => out.writeFixed64NoTag(value)
      case "sfixed64" => out.writeSFixed64NoTag(value)
      case "float" =>
        out.writeFloatNoTag(java.lang.Float.intBitsToFloat(value.toInt))
      case "double" =>
        out.writeDoubleNoTag(java.lang.Double.longBitsToDouble(value))
      case "bool" => out.writeBoolNoTag(value != 0)
    }

  def read(in: CodedInputStream, kind: String): Long = kind match {
    case "int32"    => in.readInt32().toLong
    case "uint32"   => in.readUInt32().toLong
    case "sint32"   => in.readSInt32().toLong
    case "fixed32"  => in.readFixed32().toLong
    case "sfixed32" => in.readSFixed32().toLong
    case "enum"     => in.readEnum().toLong
    case "int64"    => in.readInt64()
    case "uint64"   => in.readUInt64()
    case "sint64"   => in.readSInt64()
    case "fixed64"  => in.readFixed64()
    case "sfixed64" => in.readSFixed64()
    case "float"    => java.lang.Float.floatToIntBits(in.readFloat()).toLong
    case "double"   => java.lang.Double.doubleToLongBits(in.readDouble())
    case "bool"     => if (in.readBool()) 1L else 0L
  }

  def size(kind: String, value: Long): Int = kind match {
    case "int32"    => CodedOutputStream.computeInt32SizeNoTag(value.toInt)
    case "uint32"   => CodedOutputStream.computeUInt32SizeNoTag(value.toInt)
    case "sint32"   => CodedOutputStream.computeSInt32SizeNoTag(value.toInt)
    case "fixed32"  => CodedOutputStream.computeFixed32SizeNoTag(value.toInt)
    case "sfixed32" => CodedOutputStream.computeSFixed32SizeNoTag(value.toInt)
    case "enum"     => CodedOutputStream.computeEnumSizeNoTag(value.toInt)
    case "int64"    => CodedOutputStream.computeInt64SizeNoTag(value)
    case "uint64"   => CodedOutputStream.computeUInt64SizeNoTag(value)
    case "sint64"   => CodedOutputStream.computeSInt64SizeNoTag(value)
    case "fixed64"  => CodedOutputStream.computeFixed64SizeNoTag(value)
    case "sfixed64" => CodedOutputStream.computeSFixed64SizeNoTag(value)
    case "float" =>
      CodedOutputStream.computeFloatSizeNoTag(
        java.lang.Float.intBitsToFloat(value.toInt)
      )
    case "double" =>
      CodedOutputStream.computeDoubleSizeNoTag(
        java.lang.Double.longBitsToDouble(value)
      )
    case "bool" => CodedOutputStream.computeBoolSizeNoTag(value != 0)
  }

  val tests = Tests {
    test("Google Java fixtures agree with encoders decoders and sizes") {
      for (row <- WireFixtures.rows) {
        val parts = row.split("\\|")
        val kind = parts(0)
        val value = parts(1).toLong
        val expected = hex(parts(2))
        assert(size(kind, value) == expected.length)
        val array = new Array[Byte](expected.length)
        val flat = CodedOutputStream.newInstance(array)
        write(flat, kind, value)
        assert(array.sameElements(expected))
        for (bufferSize <- Seq(1, 2, 7, 4096)) {
          val stream = new ByteArrayOutputStream()
          val out = CodedOutputStream.newInstance(stream, bufferSize)
          write(out, kind, value)
          out.flush()
          assert(stream.toByteArray.sameElements(expected))
        }
        for (in <- CodedInputStreamSpec.inputs(expected)) {
          assert(read(in, kind) == value, in.isAtEnd)
        }
      }
    }

    test("tag boundaries and sliced byte fields") {
      for (number <- Seq(1, 15, 16, 2047, 2048, 536870911)) {
        val slice =
          ByteString.copyFrom(Array[Byte](99, 1, 2, 3, 99)).slice(1, 4)
        val wire = CodedInputStreamSpec.encoded(_.writeBytes(number, slice))
        val expected = CodedInputStreamSpec.varint(
          (number.toLong << 3) | 2
        ) ++ Array[Byte](3, 1, 2, 3)
        assert(wire.sameElements(expected))
        assert(
          CodedOutputStream.computeBytesSize(number, slice) == expected.length
        )
        for (in <- CodedInputStreamSpec.inputs(wire)) {
          assert(in.readTag() == ((number << 3) | 2))
          assert(in.readBytes() == slice, in.isAtEnd)
        }
      }
    }
  }
}
