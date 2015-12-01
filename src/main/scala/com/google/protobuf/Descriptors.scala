package com.google.protobuf

object Descriptors {
  class EnumValueDescriptor

  class EnumDescriptor {
    def getValues(): java.util.List[EnumValueDescriptor] = ???
  }

  class FieldDescriptor

  class FileDescriptor

  class Descriptor {
    def getFields(): java.util.List[FieldDescriptor] = ???
  }
}
