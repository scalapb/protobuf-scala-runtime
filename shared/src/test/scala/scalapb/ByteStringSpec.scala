package scalapb

import com.google.protobuf.ByteString
import utest._

object ByteStringSpec extends TestSuite {
  val ba1 = ByteString.copyFrom(Array[Byte](1, 2, 3))
  val ba2 = ByteString.copyFrom(Array[Byte](4, 5, 6))

  val tests = Tests {
    "ByteString.concat works" - {
      ba1.concat(ba2).toByteArray() ==> Array(1, 2, 3, 4, 5, 6)
      ByteString.empty.concat(ba1).toByteArray() ==> Array(1, 2, 3)
      ba1.concat(ByteString.empty).toByteArray() ==> Array(1, 2, 3)
    }
  }
}
