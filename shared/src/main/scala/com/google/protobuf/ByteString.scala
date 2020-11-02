package com.google.protobuf

import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

import scala.collection._

class ByteString private (bytesIn: Array[Byte], start: Int, len: Int)
    extends ByteStringParent {

  val bytes = bytesIn

  def length: Int = len

  private lazy val hash = {
    var h = length;
    {
      val upperLimit = start + len
      var i = start
      while (i < upperLimit) {
        h = h * 31 + bytes(i)
        i += 1
      }
    }
    if (h == 0) {
      h = 1
    }
    h
  }

  def apply(i: Int): Byte =
    if (i < 0 || i >= start + len)
      throw new IndexOutOfBoundsException(i.toString)
    else bytes(start + i)

  override def slice(from: Int, until: Int): ByteString = {
    val lo = math.max(from, 0)
    val hi = math.max(math.min(until, len), lo)
    new ByteString(bytes, start + lo, hi - lo)
  }

  override def equals(that: Any): Boolean = {
    that match {
      case other: ByteString =>
        other.eq(
          this
        ) || ((length == other.length) && (length == 0 || (equalsRange(other))))
      case _ => false
    }
  }

  override def hashCode() = hash

  /** Check equality of the content of two ByteStrings.
    */
  private def equalsRange(other: ByteString): Boolean =
    other.length == length && {
      var thisIndex = start
      var otherIndex = 0
      val upperLimit = start + len
      while (thisIndex < upperLimit) {
        if (bytes(thisIndex) != other.bytes(otherIndex)) {
          return false
        }
        thisIndex += 1
        otherIndex += 1
      }
      true
    }

  def writeTo(out: OutputStream) = {
    out.write(bytes, start, len)
  }

  /** Copies bytes into a buffer.
    *
    * @param target       buffer to copy into
    * @param sourceOffset offset within these bytes
    * @param targetOffset offset within the target buffer
    * @param numberToCopy number of bytes to copy
    * @throws IndexOutOfBoundsException if an offset or size is negative or too
    *                                   large
    */
  def copyTo(
      target: Array[Byte],
      sourceOffset: Int,
      targetOffset: Int,
      numberToCopy: Int
  ): Unit = {
    if (sourceOffset < 0) {
      throw new IndexOutOfBoundsException("Source offset < 0: " + sourceOffset)
    }
    if (targetOffset < 0) {
      throw new IndexOutOfBoundsException("Target offset < 0: " + targetOffset)
    }
    if (numberToCopy < 0) {
      throw new IndexOutOfBoundsException("Length < 0: " + numberToCopy)
    }
    if (sourceOffset + numberToCopy > size) {
      throw new IndexOutOfBoundsException(
        "Source end offset < 0: " + (sourceOffset + numberToCopy)
      )
    }
    if (targetOffset + numberToCopy > target.length) {
      throw new IndexOutOfBoundsException(
        "Target end offset < 0: " + (targetOffset + numberToCopy)
      )
    }
    if (numberToCopy > 0) {
      System.arraycopy(
        bytes,
        start + sourceOffset,
        target,
        targetOffset,
        numberToCopy
      );
    }
  }

  def newCodedInput(): CodedInputStream = CodedInputStream.newInstance(bytes)

  def newInput(): java.io.InputStream =
    new java.io.ByteArrayInputStream(bytes, start, len)

  private def toStringInternal(charset: Charset) = {
    new String(bytes, start, len, charset)
  }

  @throws[UnsupportedEncodingException]
  def toString(charsetName: String): String = {
    try {
      toString(Charset.forName(charsetName))
    } catch {
      case e: UnsupportedCharsetException =>
        val exception = new UnsupportedEncodingException(charsetName)
        exception.initCause(e)
        throw exception
    }
  }

  def toStringUtf8(): String = toString(Internal.UTF_8)

  def concat(other: ByteString): ByteString = {
    val ba = toByteArray() ++ other
    new ByteString(ba, 0, ba.length)
  }

  /** Constructs a new {@code String} by decoding the bytes using the
    * specified charset. Returns the same empty String if empty.
    *
    * @param charset encode using this charset
    * @return new string
    */
  def toString(charset: Charset): String = {
    if (isEmpty) "" else toStringInternal(charset)
  }

  def toByteArray(): Array[Byte] = {
    val dest = new Array[Byte](length)
    System.arraycopy(bytes, start, dest, 0, len)
    dest
  }
}

object ByteString extends ByteStringCompanionParent {

  def empty: com.google.protobuf.ByteString =
    EMPTY

  def newBuilder: mutable.Builder[Byte, ByteString] =
    Vector.newBuilder[Byte].mapResult { v: Vector[Byte] =>
      val r = new Array[Byte](v.size)
      new ByteString(v.toArray, 0, v.size)
    }

  def copyFrom(bytes: Array[Byte], offset: Int, size: Int) = {
    val copy: Array[Byte] = new Array[Byte](size)
    System.arraycopy(bytes, offset, copy, 0, size)
    new ByteString(copy, 0, size)
  }

  def copyFrom(bytes: Array[Byte]): ByteString =
    copyFrom(bytes, 0, bytes.length)

  def copyFromUtf8(text: String): ByteString =
    copyFrom(text.getBytes(Internal.UTF_8))

  private[protobuf] def useBuffer(bytes: Array[Byte]) =
    new ByteString(bytes, 0, bytes.length)

  final val EMPTY = new ByteString(new Array[Byte](0), 0, 0)

  def newOutput() = new Output()

  def newOutput(initialSize: Int) = new Output(initialSize)

  class Output(initialSize: Int = 16) extends OutputStream {
    private val bytes = new mutable.ArrayBuffer[Byte](initialSize)

    override def write(b: Int): Unit = {
      bytes += b.toByte
    }

    override def write(b: Array[Byte], offset: Int, len: Int): Unit = {
      bytes ++= b.slice(offset, offset + len)
    }

    def toByteString(): ByteString = ByteString.copyFrom(bytes.toArray)
  }
}
