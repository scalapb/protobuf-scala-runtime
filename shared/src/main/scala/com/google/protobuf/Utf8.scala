package com.google.protobuf

/** Strict UTF-8 validation shared by all platforms. Rejects overlong encodings,
  * surrogate code points, and values above U+10FFFF.
  */
private[protobuf] object Utf8 {
  def isValid(bytes: Array[Byte], offset: Int, length: Int): Boolean = {
    val end = offset + length
    var i = offset
    while (i < end) {
      val first = bytes(i) & 0xff
      i += 1
      if (first >= 0x80) {
        val count =
          if (first >= 0xc2 && first <= 0xdf) 1
          else if (first >= 0xe0 && first <= 0xef) 2
          else if (first >= 0xf0 && first <= 0xf4) 3
          else return false
        if (end - i < count) return false
        val second = bytes(i) & 0xff
        if (
          (first == 0xe0 && second < 0xa0) ||
          (first == 0xed && second >= 0xa0) ||
          (first == 0xf0 && second < 0x90) ||
          (first == 0xf4 && second >= 0x90)
        ) return false
        var remaining = count
        while (remaining > 0) {
          if ((bytes(i) & 0xc0) != 0x80) return false
          i += 1
          remaining -= 1
        }
      }
    }
    true
  }
}
