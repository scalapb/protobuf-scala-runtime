package scalapb

import com.google.protobuf.{
  ByteString,
  CodedInputStream,
  InvalidProtocolBufferException
}
import utest._

object Utf8Spec extends TestSuite {
  def bytes(xs: Int*): Array[Byte] = xs.map(_.toByte).toArray
  def valid(data: Array[Byte]): Boolean =
    ByteString.copyFrom(data).isValidUtf8()

  // Independent Unicode scalar encoder: no dependency on the production validator.
  def encode(cp: Int): Array[Byte] = {
    if (cp < 0x80) bytes(cp)
    else if (cp < 0x800) bytes(0xc0 | (cp >> 6), 0x80 | (cp & 63))
    else if (cp < 0x10000)
      bytes(0xe0 | (cp >> 12), 0x80 | ((cp >> 6) & 63), 0x80 | (cp & 63))
    else
      bytes(
        0xf0 | (cp >> 18),
        0x80 | ((cp >> 12) & 63),
        0x80 | ((cp >> 6) & 63),
        0x80 | (cp & 63)
      )
  }

  val tests = Tests {
    test("every one and two byte input") {
      assert(valid(Array.emptyByteArray))
      for (a <- 0 to 255) {
        assert(valid(bytes(a)) == (a < 128))
        for (b <- 0 to 255) {
          val expected = (a < 128 && b < 128) ||
            (a >= 0xc2 && a <= 0xdf && b >= 0x80 && b <= 0xbf)
          assert(valid(bytes(a, b)) == expected)
        }
      }
    }

    test("every Unicode scalar and surrogate code point") {
      var cp = 0
      while (cp <= 0x10ffff) {
        val expected = cp < 0xd800 || cp > 0xdfff
        assert(valid(encode(cp)) == expected)
        cp += 1
      }
    }

    test("all lead and second bytes for three and four byte sequences") {
      for (lead <- 0xe0 to 0xef; second <- 0 to 255) {
        val expected = second >= 0x80 && second <= 0xbf &&
          (lead != 0xe0 || second >= 0xa0) && (lead != 0xed || second < 0xa0)
        assert(valid(bytes(lead, second, 0x80)) == expected)
      }
      for (lead <- 0xf0 to 0xff; second <- 0 to 255) {
        val expected = lead <= 0xf4 && second >= 0x80 && second <= 0xbf &&
          (lead != 0xf0 || second >= 0x90) && (lead != 0xf4 || second < 0x90)
        assert(valid(bytes(lead, second, 0x80, 0x80)) == expected)
      }
    }

    test("truncation continuation corruption and slice isolation") {
      for (
        cp <- Seq(0x80, 0x7ff, 0x800, 0xd7ff, 0xe000, 0xffff, 0x10000, 0x10ffff)
      ) {
        val data = encode(cp)
        for (n <- 1 until data.length) assert(!valid(data.take(n)))
        for (
          index <- 1 until data.length; replacement <- 0 to 255
          if replacement < 128 || replacement > 191
        ) {
          val corrupt = data.clone()
          corrupt(index) = replacement.toByte
          assert(!valid(corrupt))
        }
        val slice = ByteString
          .copyFrom(bytes(0xff) ++ data ++ bytes(0xff))
          .slice(1, 1 + data.length)
        assert(slice.isValidUtf8())
        assert(!ByteString.copyFrom(data).slice(1, data.length).isValidUtf8())
      }
    }

    test(
      "strict string reader rejects invalid UTF8 while ordinary reader replaces it"
    ) {
      for (
        data <- Seq(
          bytes(0xff),
          bytes(0xc0, 0x80),
          bytes(0xed, 0xa0, 0x80),
          bytes(0xf4, 0x90, 0x80, 0x80)
        )
      ) {
        val wire = bytes(data.length) ++ data
        assertThrows[InvalidProtocolBufferException](
          CodedInputStream.newInstance(wire).readStringRequireUtf8()
        )
        assert(CodedInputStream.newInstance(wire).readString().nonEmpty)
      }
      for (
        text <- Seq(
          "",
          "ascii",
          "\u0000",
          "\u20ac",
          "\ud83d\ude00",
          "a\u20ac\ud83d\ude00z"
        )
      ) {
        val data = ByteString.copyFromUtf8(text).toByteArray()
        assert(
          CodedInputStream
            .newInstance(bytes(data.length) ++ data)
            .readStringRequireUtf8() == text
        )
      }
    }
  }
}
