package com.google.protobuf

import scala.collection.SpecificIterableFactory

abstract class ByteStringCompanionParent
  extends SpecificIterableFactory[Byte, ByteString] { self: ByteString.type =>

  override def fromSpecific(it: IterableOnce[Byte]): ByteString = {
    val builder = newBuilder
    builder ++= it
    builder.result
  }

}
