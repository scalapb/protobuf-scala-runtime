package compatibility

import com.google.protobuf.{ByteString, CodedInputStream, CodedOutputStream}
import java.io.ByteArrayOutputStream

// Deliberately uses only the 0.8.16 API. Compile against that artifact and run
// against the candidate without recompiling; also compile against the new API.
object PreviousReleaseConsumer {
  def main(args: Array[String]): Unit = {
    val bytes = ByteString.copyFromUtf8("hello")
    assert(bytes.bytes.sameElements(bytes.toByteArray()))
    assert(bytes.slice(1, 4).toStringUtf8() == "ell")
    assert(bytes == ByteString.copyFrom(bytes.toByteArray()))
    assert(bytes.isValidUtf8())
    // The getter must link from old bytecode while returning the new safe copy.
    val visible = ByteString.copyFrom(Array[Byte](99, 1, 2, 99)).slice(1, 3)
    val copy = visible.bytes
    assert(copy.sameElements(Array[Byte](1, 2)))
    copy(0) = 42
    assert(visible(0) == 1)
    val stream = new ByteArrayOutputStream()
    val out = CodedOutputStream.newInstance(stream, 2)
    out.writeInt32(1, -123)
    out.writeBytes(2, bytes)
    out.flush()
    val in = CodedInputStream.newInstance(stream.toByteArray)
    assert(in.readTag() == 8)
    assert(in.readInt32() == -123)
    assert(in.readTag() == 18)
    assert(in.readBytes() == bytes)
    assert(in.readTag() == 0)
    println(
      "Previous-release consumer passed on Java " + System.getProperty(
        "java.version"
      )
    )
  }
}
