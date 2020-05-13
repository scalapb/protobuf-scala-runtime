package com.google.protobuf

import scala.collection.IndexedSeqOptimized
import scala.collection.mutable.Builder

abstract class ByteStringParent
    extends IndexedSeq[Byte]
    with IndexedSeqOptimized[Byte, ByteString] { self: ByteString =>

  override def newBuilder: Builder[Byte, ByteString] =
    ByteString.newBuilder

}
