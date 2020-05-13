package com.google.protobuf

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
