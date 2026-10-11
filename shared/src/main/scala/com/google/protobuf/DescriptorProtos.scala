package com.google.protobuf

// Compilation compatibility stubs, not a functional Java protobuf message or
// reflection runtime. See README.md for the supported API and limitations.

object DescriptorProtos {
  class FileDescriptorProto

  object FileDescriptorProto {
    def parseFrom(b: Array[Byte]): FileDescriptorProto = new FileDescriptorProto
  }

  def getDescriptor(): com.google.protobuf.Descriptors.FileDescriptor = ???

  class FileOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[FileOptions]
  class MessageOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[MessageOptions]
  class FieldOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[FieldOptions]
  class EnumOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[EnumOptions]
  class EnumValueOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[EnumValueOptions]
  class ServiceOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[ServiceOptions]
  class MethodOptions
      extends Message
      with GeneratedMessageV3.ExtendableMessage[MethodOptions]
}
