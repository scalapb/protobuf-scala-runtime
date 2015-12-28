package com.google.protobuf

object Descriptors {
  class EnumValueDescriptor {
    def getName(): String = ???

    def getNumber(): Int = ???
  }

  class EnumDescriptor {
    def getName(): String = ???

    def getValues(): java.util.List[EnumValueDescriptor] = ???

    def findValueByName(name: String): EnumValueDescriptor = ???

    def findValueByNumber(number: Int): EnumValueDescriptor = ???
  }

  class FieldDescriptor {
    def getName(): String = ???

    def getNumber(): Int = ???

    def getType(): FieldDescriptor.Type = ???

    def isOptional(): Boolean = ???

    def isRepeated(): Boolean = ???

    def isRequired(): Boolean = ???
  }

  object FieldDescriptor {
    sealed trait Type
    object Type {
      case object DOUBLE extends Type
      case object FLOAT extends Type
      case object INT64 extends Type
      case object UINT64 extends Type
      case object INT32 extends Type
      case object FIXED64 extends Type
      case object FIXED32 extends Type
      case object BOOL extends Type
      case object STRING extends Type
      case object GROUP extends Type
      case object MESSAGE extends Type
      case object BYTES extends Type
      case object UINT32 extends Type
      case object ENUM extends Type
      case object SFIXED32 extends Type
      case object SFIXED64 extends Type
      case object SINT32 extends Type
      case object SINT64 extends Type
    }
  }

  class FileDescriptor

  class Descriptor {
    def getFields(): java.util.List[FieldDescriptor] = ???
  }
}
