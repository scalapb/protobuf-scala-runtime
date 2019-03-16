package com.google.protobuf

import scala.collection.mutable.Builder
import scala.collection.generic.CanBuildFrom

abstract class ByteStringCompanionParent { self: ByteString.type =>

  implicit def canBuildFrom: CanBuildFrom[ByteString, Byte, ByteString] =
    new CanBuildFrom[ByteString, Byte, ByteString] {
      def apply(from: ByteString): Builder[Byte, ByteString] = newBuilder
      def apply(): Builder[Byte, ByteString] = newBuilder
    }

}
