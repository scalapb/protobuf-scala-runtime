package scalapb

import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream
import utest._

object ByteStringSpec extends TestSuite {
  def value(xs: Int*): ByteString =
    ByteString.copyFrom(xs.map(_.toByte).toArray)

  val tests = Tests {
    test("all bounded slices and nested slices preserve bytes and bounds") {
      val data = (0 until 8).map(_.toByte).toArray
      val original = ByteString.copyFrom(data)
      for (from <- -2 to 10; until <- -2 to 10) {
        val slice = original.slice(from, until)
        val expected = data.slice(from, until)
        assert(slice.length == expected.length)
        assert(slice.toByteArray().sameElements(expected))
        assert(slice.iterator.toArray.sameElements(expected))
        for (i <- expected.indices) assert(slice(i) == expected(i))
        for (i <- Seq(-1, slice.length, Int.MaxValue, Int.MinValue))
          assertThrows[IndexOutOfBoundsException](slice(i))
        val out = new ByteArrayOutputStream()
        slice.writeTo(out)
        assert(out.toByteArray.sameElements(expected))
        val in = slice.newInput()
        expected.foreach(b => assert(in.read() == (b & 255)))
        assert(in.read() == -1)
        val coded = slice.newCodedInput()
        assert(coded.getTotalBytesRead() == 0)
        assert(coded.readRawBytes(expected.length).sameElements(expected))
        assert(coded.isAtEnd)
        assertThrows[com.google.protobuf.InvalidProtocolBufferException](
          coded.readRawByte()
        )
        for (lo <- -1 to 9; hi <- -1 to 9) {
          assert(
            slice
              .slice(lo, hi)
              .toByteArray()
              .sameElements(expected.slice(lo, hi))
          )
        }
      }
      assert(original.slice(Int.MaxValue, Int.MaxValue).isEmpty)
      assert(original.slice(Int.MinValue, Int.MaxValue) == original)
    }

    test("equality is symmetric and hashes agree for every slice") {
      val data = Array[Byte](1, 2, 1, 2, 3, 1, 2)
      val original = ByteString.copyFrom(data)
      val slices =
        for (a <- 0 to data.length; b <- a to data.length)
          yield original.slice(a, b)
      for (left <- slices; right <- slices) {
        val expected = left.toByteArray().sameElements(right.toByteArray())
        assert((left == right) == expected, (right == left) == expected)
        if (expected) assert(left.hashCode() == right.hashCode())
      }
      for (slice <- slices) {
        val copy = ByteString.copyFrom(slice.toByteArray())
        assert(
          slice == copy,
          copy == slice,
          slice.hashCode() == copy.hashCode()
        )
        assert(Set(slice).contains(copy))
        assert(Map(slice -> 42).get(copy).contains(42))
      }
    }

    test("public arrays cannot mutate a value or cached hash") {
      val source = Array[Byte](1, 2, 3)
      val b = ByteString.copyFrom(source)
      val slice = b.slice(1, 3)
      val hash = b.hashCode()
      val sliceHash = slice.hashCode()
      source(0) = 99
      b.toByteArray()(1) = 99
      b.bytes(2) = 99
      slice.bytes(0) = 99
      assert(b == value(1, 2, 3), slice == value(2, 3))
      assert(b.hashCode() == hash, slice.hashCode() == sliceHash)
    }

    test("copyTo validates offsets lengths and overflow") {
      val b = value(9, 1, 2, 3, 9).slice(1, 4)
      for (source <- -1 to 4; target <- -1 to 5; count <- -1 to 5) {
        val out = Array.fill[Byte](4)(99)
        val valid = source >= 0 && target >= 0 && count >= 0 &&
          source <= 3 - count && target <= 4 - count
        if (valid) {
          b.copyTo(out, source, target, count)
          val expected = Array.fill[Byte](4)(99)
          Array.copy(Array[Byte](1, 2, 3), source, expected, target, count)
          assert(out.sameElements(expected))
        } else {
          assertThrows[IndexOutOfBoundsException](
            b.copyTo(out, source, target, count)
          )
          assert(out.forall(_ == 99))
        }
      }
      for (
        (source, target, count) <- Seq(
          (Int.MaxValue, 0, 1),
          (0, Int.MaxValue, 1),
          (1, 1, Int.MaxValue)
        )
      )
        assertThrows[IndexOutOfBoundsException](
          b.copyTo(new Array[Byte](4), source, target, count)
        )
    }

    test("copyFrom validates array ranges before allocating") {
      val data = Array[Byte](1, 2, 3)
      for (offset <- -1 to 4; size <- -1 to 4) {
        if (offset >= 0 && size >= 0 && offset <= data.length - size)
          assert(
            ByteString
              .copyFrom(data, offset, size)
              .toByteArray()
              .sameElements(data.slice(offset, offset + size))
          )
        else
          assertThrows[IndexOutOfBoundsException](
            ByteString.copyFrom(data, offset, size)
          )
      }
      assertThrows[IndexOutOfBoundsException](
        ByteString.copyFrom(data, 1, Int.MaxValue)
      )
      assertThrows[IndexOutOfBoundsException](
        ByteString.copyFrom(data, Int.MaxValue, 1)
      )
    }

    test("concat builders and output preserve slices and snapshots") {
      val values =
        Seq(ByteString.empty, value(1), value(9, 2, 3, 9).slice(1, 3))
      for (a <- values; b <- values)
        assert(
          a.concat(b)
            .toByteArray()
            .sameElements(a.toByteArray() ++ b.toByteArray())
        )
      val builder = ByteString.newBuilder
      builder ++= Array[Byte](1, 2, 3)
      assert(builder.result() == value(1, 2, 3))
      val out = ByteString.newOutput()
      out.write(Array[Byte](9, 1, 2, 9), 1, 2)
      val snapshot = out.toByteString()
      out.write(3)
      assert(snapshot == value(1, 2), out.toByteString() == value(1, 2, 3))
      for ((offset, len) <- Seq((-1, 1), (0, -1), (2, 2), (1, Int.MaxValue)))
        assertThrows[IndexOutOfBoundsException](
          out.write(Array[Byte](1, 2, 3), offset, len)
        )
    }
  }
}
