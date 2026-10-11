package scalapb

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.CodedOutputStream.OutOfSpaceException
import java.io.{ByteArrayOutputStream, IOException, OutputStream}
import java.nio.{ByteBuffer, BufferOverflowException, ReadOnlyBufferException}
import utest._

object CodedOutputStreamSpec extends TestSuite {
  val tests = Tests {
    test("reject empty streaming buffers including the public constructor") {
      for (size <- Seq(Int.MinValue, -1, 0)) {
        assertThrows[IllegalArgumentException] {
          CodedOutputStream.newInstance(new ByteArrayOutputStream(), size)
        }
        assertThrows[IllegalArgumentException] {
          CodedOutputStream.newInstance(ByteBuffer.allocate(1), size)
        }
      }
      assertThrows[IllegalArgumentException] {
        new CodedOutputStream(new ByteArrayOutputStream(), Array.emptyByteArray)
      }
      val empty = CodedOutputStream.newInstance(Array.emptyByteArray)
      empty.writeRawBytes(Array.emptyByteArray)
      empty.checkNoSpaceLeft()
      assertThrows[OutOfSpaceException](empty.writeRawByte(1))
    }

    test("all buffer representations preserve source position limit and mark") {
      for (
        length <- (0 to 20) ++ Seq(4095, 4096, 4097, 8193);
        bufferSize <- Seq(1, 2, 7, 4096)
      ) {
        val expected = Array.tabulate[Byte](length)(_.toByte)
        val heap = ByteBuffer.wrap(expected)
        val direct = ByteBuffer.allocateDirect(length)
        direct.put(expected).clear()
        val padded =
          ByteBuffer.wrap(Array[Byte](99) ++ expected ++ Array[Byte](98))
        padded.position(1).limit(length + 1)
        for (
          source <- Seq(heap, direct, padded.slice())
            .flatMap(b => Seq(b, b.asReadOnlyBuffer()))
        ) {
          val position = length / 2
          source.position(position).mark().limit(math.max(position, length - 1))
          val originalLimit = source.limit()
          val stream = new ByteArrayOutputStream()
          val out = CodedOutputStream.newInstance(stream, bufferSize)
          out.writeRawByte(42)
          out.writeRawBytes(source)
          out.flush()
          assert(stream.toByteArray.sameElements(Array[Byte](42) ++ expected))
          assert(source.position() == position, source.limit() == originalLimit)
          source.reset()
          assert(source.position() == position)
        }
      }
    }

    test("validate every array range before writing tags lengths or payload") {
      val data = Array[Byte](10, 11, 12, 13)
      val boundaries = (-2 to 6) ++ Seq(Int.MinValue, Int.MaxValue)
      for (
        offset <- boundaries; length <- boundaries; bufferSize <- Seq(1, 2, 7);
        mode <- 0 to 2
      ) {
        val stream = new ByteArrayOutputStream()
        val out = CodedOutputStream.newInstance(stream, bufferSize)
        out.writeRawByte(42)
        def write(): Unit = mode match {
          case 0 => out.writeRawBytes(data, offset, length)
          case 1 => out.writeByteArrayNoTag(data, offset, length)
          case 2 => out.writeByteArray(1, data, offset, length)
        }
        if (offset < 0 || length < 0 || offset.toLong + length > data.length) {
          assertThrows[IndexOutOfBoundsException](write())
          out.flush()
          assert(stream.toByteArray.sameElements(Array[Byte](42)))
        } else {
          write()
          out.flush()
          val prefix = mode match {
            case 0 => Array.emptyByteArray
            case 1 => Array(length.toByte)
            case 2 => Array[Byte](10, length.toByte)
          }
          assert(
            stream.toByteArray.sameElements(
              Array[Byte](42) ++ prefix ++ data.slice(offset, offset + length)
            )
          )
        }
      }
    }

    test("flat arrays exhaust at the exact boundary") {
      for (size <- 0 to 20; length <- 0 to 21) {
        val data = Array.tabulate[Byte](length)(_.toByte)
        val target = new Array[Byte](size)
        val out = CodedOutputStream.newInstance(target)
        if (length > size)
          assertThrows[OutOfSpaceException](out.writeRawBytes(data))
        else {
          out.writeRawBytes(data)
          assert(out.spaceLeft() == size - length)
          if (size == length) out.checkNoSpaceLeft()
          else assertThrows[IllegalStateException](out.checkNoSpaceLeft())
        }
        assert(
          target.take(math.min(size, length)).sameElements(data.take(size))
        )
      }
    }

    test("ByteBuffer destinations respect slices limits and writability") {
      val backing = Array.fill[Byte](8)(99)
      val parent = ByteBuffer.wrap(backing)
      parent.position(2).limit(6)
      val target = parent.slice()
      val out = CodedOutputStream.newInstance(target, 2)
      out.writeRawBytes(Array[Byte](1, 2, 3, 4))
      out.flush()
      assert(backing.sameElements(Array[Byte](99, 99, 1, 2, 3, 4, 99, 99)))
      out.writeRawByte(5)
      assertThrows[BufferOverflowException](out.flush())
      val readOnly =
        CodedOutputStream.newInstance(ByteBuffer.allocate(1).asReadOnlyBuffer())
      readOnly.writeRawByte(1)
      assertThrows[ReadOnlyBufferException](readOnly.flush())
    }

    test("propagate stream failures from flush and bulk writes") {
      for (bufferSize <- Seq(1, 2, 7); mode <- 0 to 2; failAfter <- 0 to 9) {
        val failure = new IOException("deliberate failure")
        val accepted = new ByteArrayOutputStream()
        val stream = new OutputStream {
          def write(value: Int): Unit = {
            if (accepted.size() == failAfter) throw failure
            accepted.write(value)
          }
        }
        val out = CodedOutputStream.newInstance(stream, bufferSize)
        val data = Array.tabulate[Byte](10)(_.toByte)
        val thrown = assertThrows[IOException] {
          mode match {
            case 0 => data.foreach(b => out.writeRawByte(b))
            case 1 => out.writeRawBytes(data)
            case 2 =>
              out.writeRawBytes(ByteBuffer.wrap(data).asReadOnlyBuffer())
          }
          out.flush()
        }
        assert(thrown eq failure)
        assert(accepted.toByteArray.sameElements(data.take(failAfter)))
      }
    }
  }
}
