package releasecheck

import com.google.protobuf.{
  ByteString,
  CodedInputStream,
  CodedOutputStream,
  InvalidProtocolBufferException
}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import releasecheck.release_check.Envelope
import utest._

object GeneratedMessageSpec extends TestSuite {
  def encoded(write: CodedOutputStream => Unit): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    val out = CodedOutputStream.newInstance(stream, 1)
    write(out)
    out.flush()
    stream.toByteArray
  }
  val child = Envelope.Child(
    "nested",
    ByteString.copyFrom(Array[Byte](99, 1, 2, 3, 99)).slice(1, 4)
  )
  val message = Envelope(
    child = Some(child),
    numbers = Seq(0L, -1L, 1L, Long.MinValue, Long.MaxValue),
    entries = Map("first" -> child, "empty" -> Envelope.Child()),
    choice = Envelope.Choice.Selected(child)
  )
  val tests = Tests {
    test(
      "generated messages round trip through arrays slices and chunked streams"
    ) {
      for (original <- Seq(Envelope(), message, message.withText("hello"))) {
        val wire = original.toByteArray
        assert(wire.length == original.serializedSize)
        val fromArray = Envelope.parseFrom(wire)
        assert(fromArray == original)
        val sliced = ByteString
          .copyFrom(Array[Byte](99) ++ wire ++ Array[Byte](98))
          .slice(1, wire.length + 1)
        val fromSlice = Envelope.parseFrom(sliced.newCodedInput())
        assert(fromSlice == original)
        for (chunk <- Seq(1, 2, 7, 4096)) {
          val stream = new ByteArrayInputStream(wire) {
            override def read(b: Array[Byte], offset: Int, length: Int): Int =
              super.read(b, offset, math.min(chunk, length))
          }
          val fromStream =
            Envelope.parseFrom(CodedInputStream.newInstance(stream))
          assert(fromStream == original)
        }
      }
    }
    test("packed and unpacked values concatenate and last oneof wins") {
      val wire = message.toByteArray ++ encoded { out =>
        out.writeSInt64(2, 42)
        out.writeString(4, "last")
      }
      val parsed = Envelope.parseFrom(wire)
      assert(parsed.numbers == message.numbers :+ 42L)
      val expectedChoice = Envelope.Choice.Text("last")
      assert(parsed.choice == expectedChoice)
    }
    test("unknown fields survive generated message parse and serialization") {
      val extra = encoded { out =>
        out.writeUInt64(100, Long.MaxValue)
        out.writeFixed32(101, 123)
        out.writeFixed64(102, 456L)
        out.writeBytes(103, ByteString.copyFromUtf8("unknown"))
      }
      val parsed = Envelope.parseFrom(message.toByteArray ++ extra)
      assert(parsed.unknownFields != scalapb.UnknownFieldSet.empty)
      val roundTrip = Envelope.parseFrom(parsed.toByteArray)
      assert(
        roundTrip == parsed,
        roundTrip.unknownFields == parsed.unknownFields
      )
      assert(parsed.withUnknownFields(scalapb.UnknownFieldSet.empty) == message)
    }
    test("malformed nested packed and unknown payloads are rejected") {
      val malformed = Seq(
        Array[Byte](10, 5, 10, 3, 97), // truncated child
        Array[Byte](18, 1, -128), // truncated packed varint
        Array[Byte](18, -1, -1, -1, -1, 15), // negative packed length
        Array[Byte](0), // invalid tag
        Array[Byte](15), // invalid wire type
        Array[Byte](-94, 6, 4, 1) // truncated unknown bytes
      )
      for (wire <- malformed) {
        def parseArray() = Envelope.parseFrom(wire)
        assertThrows[InvalidProtocolBufferException](parseArray())
        def parseStream() = Envelope.parseFrom(new ByteArrayInputStream(wire))
        assertThrows[InvalidProtocolBufferException](parseStream())
      }
    }
    test("downstream source compatibility") {
      compatibility.PreviousReleaseConsumer.main(Array.empty[String])
    }
  }
}
