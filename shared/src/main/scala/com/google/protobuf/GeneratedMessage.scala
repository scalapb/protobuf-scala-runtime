package com.google.protobuf

// Compilation compatibility stubs, not a functional Java protobuf message or
// reflection runtime. See README.md for the supported API and limitations.

trait Message {
  def getUnknownFields(): UnknownFieldSet = ???
}

trait GeneratedMessage extends Message {}

object GeneratedMessage {
  trait ExtendableMessage[S]
}

class UnknownFieldSet {
  def getField(number: Int): UnknownFieldSet.Field = ???
}

object UnknownFieldSet {
  class Field {
    def getLengthDelimitedList(): java.util.List[ByteString] = ???
  }
}
