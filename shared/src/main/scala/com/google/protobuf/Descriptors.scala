package com.google.protobuf

import com.google.protobuf.DescriptorProtos._

object Descriptors {
  class EnumValueDescriptor {
    def getName(): String = throw new NotImplementedError("EnumValueDescriptor.getName not implemented")

    def getNumber(): Int = throw new NotImplementedError("EnumValueDescriptor.getNumber not implemented")
  }

  class EnumDescriptor {
    def getName(): String = throw new NotImplementedError("EnumDescriptor.getName not implemented")

    def getContainingType(): Descriptor = throw new NotImplementedError("EnumDescriptor.getContainingType not implemented")

    def getValues(): java.util.List[EnumValueDescriptor] = throw new NotImplementedError("EnumDescriptor.getValues not implemented")

    def findValueByName(name: String): EnumValueDescriptor = throw new NotImplementedError("EnumDescriptor.findValueByName not implemented")

    def findValueByNumber(number: Int): EnumValueDescriptor = throw new NotImplementedError("EnumDescriptor.findValueByNumber not implemented")
  }

  class FieldDescriptor {
    def getName(): String = throw new NotImplementedError("FieldDescriptor.getName not implemented")

    def getContainingType(): Descriptor = throw new NotImplementedError("FieldDescriptor.getContainingType not implemented")

    def getNumber(): Int = throw new NotImplementedError("FieldDescriptor.getNumber not implemented")

    def getType(): FieldDescriptor.Type = throw new NotImplementedError("FieldDescriptor.getType not implemented")

    def isOptional(): Boolean = throw new NotImplementedError("FieldDescriptor.isOptional not implemented")

    def isRepeated(): Boolean = throw new NotImplementedError("FieldDescriptor.isRepeated not implemented")

    def isRequired(): Boolean = throw new NotImplementedError("FieldDescriptor.isRequired not implemented")
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

  class MethodDescriptor {}

  class ServiceDescriptor {
    def getMethods: java.util.List[MethodDescriptor] = {
      java.util.Arrays.asList(new MethodDescriptor)
    }
  }

  class FileDescriptor {
    def getMessageTypes(): java.util.List[Descriptor] = throw new NotImplementedError("FileDescriptor.getMessageTypes not implemented")

    def getEnumTypes(): java.util.List[EnumDescriptor] = throw new NotImplementedError("FileDescriptor.getEnumTypes not implemented")

    def getServices(): java.util.List[ServiceDescriptor] = java.util.Arrays.asList(new ServiceDescriptor)
  }

  object FileDescriptor {
    def buildFrom(p: FileDescriptorProto, deps: Array[FileDescriptor]): FileDescriptor = new FileDescriptor
  }

  class Descriptor {
    def getContainingType(): Descriptor = throw new NotImplementedError("Descriptor.getContainingType not implemented")

    def getFields(): java.util.List[FieldDescriptor] = throw new NotImplementedError("Descriptor.getFields not implemented")

    def getNestedTypes(): java.util.List[Descriptor] = throw new NotImplementedError("Descriptor.getNestedTypes not implemented")

    def getEnumTypes(): java.util.List[EnumDescriptor] = throw new NotImplementedError("Descriptor.getEnumTypes not implemented")

    def getFullName(): String = throw new NotImplementedError("Descriptor.getFullName not implemented")
  }
}
