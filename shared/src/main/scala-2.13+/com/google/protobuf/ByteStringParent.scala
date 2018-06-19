package com.google.protobuf

import scala.collection.immutable.{AbstractSeq, StrictOptimizedSeqOps, IndexedSeqOps, IndexedSeq}

abstract class ByteStringParent extends AbstractSeq[Byte]
  with IndexedSeq[Byte]
  with IndexedSeqOps[Byte, IndexedSeq, ByteString]
  with StrictOptimizedSeqOps[Byte, IndexedSeq, ByteString] { self: ByteString =>

  override def fromSpecific(coll: IterableOnce[Byte]): ByteString =
    ByteString.fromSpecific(coll)
  override def newSpecificBuilder: collection.mutable.Builder[Byte, ByteString] =
    ByteString.newBuilder
}
