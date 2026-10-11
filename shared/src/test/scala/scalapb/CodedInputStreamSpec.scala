package scalapb

import com.google.protobuf.{
  CodedInputStream,
  CodedOutputStream,
  InvalidProtocolBufferException
}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import utest._

object CodedInputStreamSpec extends TestSuite {
  def bytes(xs: Int*): Array[Byte] = xs.map(_.toByte).toArray
  def stream(data: Array[Byte], chunk: Int = Int.MaxValue): CodedInputStream =
    CodedInputStream.newInstance(new ByteArrayInputStream(data) {
      override def read(out: Array[Byte], off: Int, len: Int): Int =
        super.read(out, off, math.min(len, chunk))
    })
  def inputs(data: Array[Byte]): Seq[CodedInputStream] =
    Seq(
      CodedInputStream.newInstance(data),
      CodedInputStream.newInstance(
        bytes(99) ++ data ++ bytes(99),
        1,
        data.length
      )
    ) ++
      Seq(1, 2, 7, 4096).map(stream(data, _))

  // Independent unsigned varint encoder, used to exercise both fast and slow readers.
  def varint(value: Long): Array[Byte] = {
    var n = value
    val result = scala.collection.mutable.ArrayBuffer.empty[Byte]
    while ((n & ~0x7fL) != 0) {
      result += ((n & 127) | 128).toByte
      n = n >>> 7
    }
    result += n.toByte
    result.toArray
  }
  def encoded(f: CodedOutputStream => Unit): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val coded = CodedOutputStream.newInstance(out)
    f(coded)
    coded.flush()
    out.toByteArray
  }

  val tests = Tests {
    test("last tag tracks reads and resets at end of input") {
      for (in <- inputs(bytes(8, 42, 16, 7))) {
        assert(in.getLastTag() == 0)
        assert(in.readTag() == 8)
        assert(in.getLastTag() == 8)
        assert(in.readInt32() == 42)
        assert(in.getLastTag() == 8)
        assert(in.readTag() == 16)
        assert(in.getLastTag() == 16)
        assert(in.readInt32() == 7)
        assert(in.readTag() == 0)
        assert(in.getLastTag() == 0)
      }
    }

    test("varint boundaries every bit and deterministic random values") {
      val random = new scala.util.Random(0x5ca1abL)
      val values = Seq(0L, -1L, Long.MinValue, Long.MaxValue) ++
        (0 until 64).flatMap(b => Seq((1L << b) - 1, 1L << b, (1L << b) + 1)) ++
        Seq.fill(1000)(random.nextLong())
      for (n <- values) {
        val wire = varint(n)
        for (
          padding <- Seq(0, 10);
          in <- inputs(wire ++ Array.fill[Byte](padding)(0))
        ) {
          assert(in.readRawVarint64() == n)
          assert(in.getTotalBytesRead() == wire.length)
        }
        for (in <- inputs(wire ++ Array.fill[Byte](10)(0))) {
          assert(in.readRawVarint32() == n.toInt)
          assert(in.getTotalBytesRead() == wire.length)
        }
        assert(
          CodedInputStream.readRawVarint32(
            new ByteArrayInputStream(wire)
          ) == n.toInt
        )
        assert(encoded(_.writeUInt64NoTag(n)).sameElements(wire))
      }
    }

    test("varints crossing each buffer boundary position") {
      for (
        offset <- 4086 to 4097;
        n <- Seq(0L, 128L, Int.MaxValue.toLong, Long.MaxValue, -1L)
      ) {
        val wire = varint(n)
        val in = stream(Array.fill[Byte](offset)(0) ++ wire ++ bytes(42))
        in.skipRawBytes(offset)
        assert(in.readRawVarint64() == n)
        assert(in.readRawByte() == 42)
      }
    }

    test("every varint truncation and overlong continuation fails") {
      for (n <- Seq(128L, 16384L, Int.MaxValue.toLong, Long.MaxValue, -1L)) {
        val wire = varint(n)
        for (length <- 0 until wire.length) {
          for (in <- inputs(wire.take(length)))
            assertThrows[InvalidProtocolBufferException](in.readRawVarint64())
          for (in <- inputs(wire.take(length)))
            assertThrows[InvalidProtocolBufferException](in.readRawVarint32())
          assertThrows[InvalidProtocolBufferException](
            CodedInputStream.readRawVarint32(
              new ByteArrayInputStream(wire.take(length))
            )
          )
        }
      }
      for (padding <- Seq(0, 1, 10)) {
        val bad =
          Array.fill[Byte](10)(0x80.toByte) ++ Array.fill[Byte](padding)(0)
        for (in <- inputs(bad))
          assertThrows[InvalidProtocolBufferException](in.readRawVarint64())
        for (in <- inputs(bad))
          assertThrows[InvalidProtocolBufferException](in.readRawVarint32())
        for (in <- inputs(bytes(8) ++ bad)) {
          val tag = in.readTag()
          assertThrows[InvalidProtocolBufferException](in.skipField(tag))
        }
        assertThrows[InvalidProtocolBufferException](
          CodedInputStream.readRawVarint32(new ByteArrayInputStream(bad))
        )
      }
    }

    test("raw reads and skips around buffer boundaries and truncation") {
      for (size <- Seq(0, 1, 7, 4095, 4096, 4097, 8191, 8192, 8193)) {
        val data = Array.tabulate[Byte](size)(i => (i * 31).toByte)
        for (in <- inputs(data)) {
          assert(in.readRawBytes(size).sameElements(data), in.isAtEnd)
        }
        for (in <- inputs(data)) {
          in.skipRawBytes(size)
          assert(in.getTotalBytesRead() == size, in.isAtEnd)
        }
        if (size > 0) {
          for (in <- inputs(data.dropRight(1)))
            assertThrows[InvalidProtocolBufferException](in.readRawBytes(size))
          for (in <- inputs(data.dropRight(1)))
            assertThrows[InvalidProtocolBufferException](in.skipRawBytes(size))
        }
      }
    }

    test("negative and overflow lengths never allocate or escape limits") {
      for (in <- inputs(bytes(1, 2, 3))) {
        assertThrows[InvalidProtocolBufferException](in.readRawBytes(-1))
        assertThrows[InvalidProtocolBufferException](in.skipRawBytes(-1))
        assertThrows[InvalidProtocolBufferException](in.pushLimit(-1))
        assert(in.readRawByte() == 1)
        assertThrows[InvalidProtocolBufferException](in.pushLimit(Int.MaxValue))
        assert(in.readRawByte() == 2)
        assertThrows[InvalidProtocolBufferException](
          in.readRawBytes(Int.MaxValue)
        )
      }
      for (in <- inputs(bytes(1, 2, 3))) {
        in.readRawByte()
        assertThrows[InvalidProtocolBufferException](
          in.skipRawBytes(Int.MaxValue)
        )
      }
      for (length <- Seq(-1L, Int.MinValue.toLong, Int.MaxValue.toLong)) {
        val wire = varint(length)
        for (in <- inputs(wire))
          assertThrows[InvalidProtocolBufferException](in.readBytes())
        for (in <- inputs(wire))
          assertThrows[InvalidProtocolBufferException](in.readString())
      }
      val data = bytes(1, 2, 3)
      for (
        (off, len) <- Seq(
          (-1, 1),
          (0, -1),
          (3, 1),
          (1, Int.MaxValue),
          (Int.MaxValue, 1)
        )
      ) {
        assertThrows[IndexOutOfBoundsException](
          CodedInputStream.newInstance(data, off, len)
        )
        assertThrows[IndexOutOfBoundsException](
          new CodedInputStream(data, off, len)
        )
      }
    }

    test("nested push and pop limits restore buffered bytes") {
      for (in <- inputs(bytes(1, 2, 3, 4, 5))) {
        assert(in.readRawByte() == 1)
        val outer = in.pushLimit(3)
        assert(in.getBytesUntilLimit == 3)
        val inner = in.pushLimit(1)
        assert(in.readRawByte() == 2, in.isAtEnd)
        assertThrows[InvalidProtocolBufferException](in.readRawByte())
        in.popLimit(inner)
        assert(in.readRawByte() == 3, in.readRawByte() == 4, in.isAtEnd)
        in.popLimit(outer)
        assert(in.readRawByte() == 5, in.isAtEnd)
      }
      for (in <- inputs(Array.fill[Byte](8193)(42))) {
        val old = in.pushLimit(4097)
        assertThrows[InvalidProtocolBufferException](in.readRawBytes(4098))
        assert(in.isAtEnd)
        in.popLimit(old)
        assert(in.readRawBytes(4096).forall(_ == 42), in.isAtEnd)
      }
    }

    test("size limits cover every buffered read path") {
      val operations: Seq[(Array[Byte], CodedInputStream => Any)] = Seq(
        (bytes(1), _.readRawByte()),
        (bytes(0x80, 1), _.readRawVarint32()),
        (varint(-1L), _.readRawVarint64()),
        (bytes(1, 2, 3, 4), _.readFixed32()),
        (bytes(1, 2, 3, 4, 5, 6, 7, 8), _.readFixed64()),
        (bytes(1, 2, 3), _.readRawBytes(3)),
        (bytes(1, 2, 3), _.skipRawBytes(3)),
        (bytes(3, 65, 66, 67), _.readString()),
        (bytes(3, 65, 66, 67), _.readStringRequireUtf8()),
        (bytes(3, 65, 66, 67), _.readBytes()),
        (bytes(8, 0x80, 1), in => in.skipField(in.readTag()))
      )
      for (
        (wire, read) <- operations; prefill <- Seq(false, true);
        allowance <- 0 to wire.length
      ) {
        val in = stream(
          (if (prefill) bytes(99) else bytes()) ++ wire ++ Array.fill[Byte](16)(
            0
          )
        )
        val prefix = if (prefill) 1 else 0
        if (prefill) assert(in.readRawByte() == 99)
        in.setSizeLimit(prefix + allowance)
        if (allowance < wire.length) {
          val error = assertThrows[InvalidProtocolBufferException](read(in))
          assert(error.getMessage.contains("too large"))
          assert(in.getTotalBytesRead() <= prefix + allowance)
        } else {
          read(in)
          assert(in.getTotalBytesRead() == prefix + wire.length)
          assertThrows[InvalidProtocolBufferException](in.readRawByte())
        }
      }
    }

    test("large reads skips and length delimited fields respect size limits") {
      for (
        size <- Seq(4095, 4096, 4097, 8193); chunk <- Seq(1, 4096);
        prefill <- Seq(false, true)
      ) {
        val data = Array.fill[Byte](size)(65)
        for (
          read <- Seq[CodedInputStream => Any](
            _.readRawBytes(size),
            _.skipRawBytes(size)
          )
        ) {
          val in = stream(bytes(99) ++ data, chunk)
          if (prefill) in.readRawByte()
          in.setSizeLimit(size - 1)
          assertThrows[InvalidProtocolBufferException](read(in))
        }
        for (
          read <- Seq[CodedInputStream => Any](
            _.readBytes(),
            _.readString(),
            _.readStringRequireUtf8()
          )
        ) {
          val wire = varint(size) ++ data
          val in = stream(wire, chunk)
          in.setSizeLimit(wire.length - 1)
          assertThrows[InvalidProtocolBufferException](read(in))
          val exact = stream(wire, chunk)
          exact.setSizeLimit(wire.length)
          read(exact)
          assert(exact.getTotalBytesRead() == wire.length, exact.isAtEnd)
        }
      }
    }

    test("size limit setters reset and array exemption") {
      val in = stream(bytes(1, 2, 3, 4))
      assert(in.setSizeLimit(2) == (64 << 20))
      assert(in.readRawByte() == 1, in.readRawByte() == 2)
      assertThrows[InvalidProtocolBufferException](in.isAtEnd)
      in.resetSizeCounter()
      assert(in.getTotalBytesRead() == 0)
      assert(in.readRawByte() == 3, in.readRawByte() == 4, in.isAtEnd)
      assert(in.setSizeLimit(3) == 2)
      assertThrows[IllegalArgumentException](in.setSizeLimit(-1))
      val array = CodedInputStream.newInstance(bytes(1, 2))
      array.setSizeLimit(0)
      assert(array.readRawBytes(2).sameElements(bytes(1, 2)))
      val empty = stream(bytes())
      empty.setSizeLimit(0)
      assert(empty.isAtEnd)
    }

    test("raising the size limit after an error preserves embedded limits") {
      val in = stream(bytes(1, 2, 3, 4))
      val old = in.pushLimit(1)
      in.setSizeLimit(0)
      assertThrows[InvalidProtocolBufferException](in.isAtEnd)
      in.setSizeLimit(4)
      assert(in.readRawByte() == 1, in.isAtEnd)
      assertThrows[InvalidProtocolBufferException](in.readRawByte())
      in.popLimit(old)
      assert(in.readRawBytes(3).sameElements(bytes(2, 3, 4)), in.isAtEnd)
    }

    test("group recursion boundaries and malformed ends") {
      def groups(depth: Int) =
        Array.fill[Byte](depth)(11) ++ Array.fill[Byte](depth)(12)
      for (
        limit <- Seq(0, 1, 2, 10, 100);
        depth <- Seq(0, 1, 2, 9, 10, 11, 99, 100, 101, 1000)
      ) {
        for (in <- inputs(groups(depth))) {
          assert(in.setRecursionLimit(limit) == 100)
          if (depth <= limit) { in.skipMessage(); assert(in.isAtEnd) }
          else {
            val error =
              assertThrows[InvalidProtocolBufferException](in.skipMessage())
            assert(error.getMessage.contains("nesting"))
          }
        }
      }
      for (in <- inputs(groups(101)))
        assertThrows[InvalidProtocolBufferException](in.skipMessage())
      for (wire <- Seq(bytes(11), bytes(11, 20)))
        for (in <- inputs(wire))
          assertThrows[InvalidProtocolBufferException](in.skipMessage())
      val in = stream(bytes(11, 20, 11, 12))
      in.setRecursionLimit(1)
      assertThrows[InvalidProtocolBufferException](in.skipMessage())
      in.skipMessage() // Exception unwinding must restore the depth counter.
      assert(in.isAtEnd)
      assertThrows[IllegalArgumentException](in.setRecursionLimit(-1))
      assert(in.setRecursionLimit(2) == 1)
    }

    test("invalid tags wire types fixed-width truncation and all skip types") {
      for (tag <- 0 to 7; in <- inputs(bytes(tag)))
        assertThrows[InvalidProtocolBufferException](in.readTag())
      for (tag <- Seq(14, 15); in <- inputs(bytes(tag)))
        assertThrows[InvalidProtocolBufferException](in.skipField(in.readTag()))
      for (length <- 0 until 8; in <- inputs(new Array[Byte](length)))
        assertThrows[InvalidProtocolBufferException](in.readFixed64())
      for (length <- 0 until 4; in <- inputs(new Array[Byte](length)))
        assertThrows[InvalidProtocolBufferException](in.readFixed32())
      val wire = bytes(8, 150, 1, 17) ++ new Array[Byte](8) ++ bytes(26, 3, 1,
        2, 3, 35, 36, 45) ++ new Array[Byte](4)
      for (in <- inputs(wire)) { in.skipMessage(); assert(in.isAtEnd) }
    }

    test("broken InputStreams fail rather than looping on large reads") {
      for (result <- Seq(0, -2, 5000); size <- Seq(1, 4096, 4097)) {
        val in = CodedInputStream.newInstance(new InputStream {
          override def read(): Int = 0
          override def read(out: Array[Byte], off: Int, len: Int): Int = result
        })
        assertThrows[IllegalStateException](in.readRawBytes(size))
      }
    }
  }
}
