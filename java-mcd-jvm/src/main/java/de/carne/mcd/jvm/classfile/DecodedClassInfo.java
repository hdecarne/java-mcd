/*
 * Copyright (c) 2019-2020 Holger de Carne and contributors, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.carne.mcd.jvm.classfile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.carne.mcd.io.MCDInputBuffer;
import de.carne.mcd.jvm.classfile.attribute.Attribute;
import de.carne.mcd.jvm.classfile.attribute.CodeAttribute;
import de.carne.mcd.jvm.classfile.attribute.ConstantValueAttribute;
import de.carne.mcd.jvm.classfile.attribute.ExceptionsAttribute;
import de.carne.mcd.jvm.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import de.carne.mcd.jvm.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import de.carne.mcd.jvm.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import de.carne.mcd.jvm.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import de.carne.mcd.jvm.classfile.attribute.SignatureAttribute;
import de.carne.mcd.jvm.classfile.attribute.SourceFileAttribute;
import de.carne.mcd.jvm.classfile.attribute.annotation.Annotation;
import de.carne.mcd.jvm.classfile.attribute.annotation.AnnotationAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.AnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.AnnotationElementValue;
import de.carne.mcd.jvm.classfile.attribute.annotation.ArrayAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.BooleanAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.ByteAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.CharAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.ClassAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.DoubleAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.EnumAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.FloatAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.IntAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.LongAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.ShortAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.StringAnnotationElement;
import de.carne.mcd.jvm.classfile.attribute.annotation.TypeAnnotation;
import de.carne.mcd.jvm.classfile.attribute.annotation.TypeAnnotationPath;
import de.carne.mcd.jvm.classfile.attribute.annotation.TypeAnnotationTarget;
import de.carne.mcd.jvm.classfile.constant.ClassConstant;
import de.carne.mcd.jvm.classfile.constant.Constant;
import de.carne.mcd.jvm.classfile.constant.DoubleConstant;
import de.carne.mcd.jvm.classfile.constant.DynamicConstant;
import de.carne.mcd.jvm.classfile.constant.FieldRefConstant;
import de.carne.mcd.jvm.classfile.constant.FloatConstant;
import de.carne.mcd.jvm.classfile.constant.IntegerConstant;
import de.carne.mcd.jvm.classfile.constant.InterfaceMethodRefConstant;
import de.carne.mcd.jvm.classfile.constant.InvokeDynamicConstant;
import de.carne.mcd.jvm.classfile.constant.LongConstant;
import de.carne.mcd.jvm.classfile.constant.MethodHandleConstant;
import de.carne.mcd.jvm.classfile.constant.MethodRefConstant;
import de.carne.mcd.jvm.classfile.constant.MethodTypeConstant;
import de.carne.mcd.jvm.classfile.constant.ModuleConstant;
import de.carne.mcd.jvm.classfile.constant.NameAndTypeConstant;
import de.carne.mcd.jvm.classfile.constant.PackageConstant;
import de.carne.mcd.jvm.classfile.constant.ReferenceKind;
import de.carne.mcd.jvm.classfile.constant.StringConstant;
import de.carne.mcd.jvm.classfile.constant.Utf8Constant;
import de.carne.util.Late;
import de.carne.util.logging.Log;

/**
 * The actual class file decoder.
 */
public class DecodedClassInfo implements ClassInfo {

	private static final Log LOG = new Log();

	private int minorVersion = -1;
	private int majorVersion = -1;
	private final Map<Integer, Constant> constantPool = new HashMap<>();
	private int accessFlags = -1;
	private final Late<ClassName> thisClassNameHolder = new Late<>();
	private final Late<ClassName> superClassNameHolder = new Late<>();
	private final Late<List<ClassName>> interfacesHolder = new Late<>();
	private final Late<List<FieldInfo>> fieldsHolder = new Late<>();
	private final Late<List<MethodInfo>> methodsHolder = new Late<>();
	private final Late<List<Attribute>> attributesHolder = new Late<>();

	private DecodedClassInfo() {
		// Prevent instantiation outside this class
	}

	/**
	 * Decodes the given class file data.
	 *
	 * @param buffer the class file data to decode.
	 * @return the decoded class info.
	 * @throws IOException if a decoding failure occurs.
	 */
	public static DecodedClassInfo decode(MCDInputBuffer buffer) throws IOException {
		return new DecodedClassInfo().decode0(buffer);
	}

	private DecodedClassInfo decode0(MCDInputBuffer buffer) throws IOException {
		buffer.decodeMagic(0xcafebabe);
		decodeClass(buffer);
		decodeFields(buffer);
		decodeMethods(buffer);
		decodeClassAttributes(buffer);
		return this;
	}

	private void decodeClass(MCDInputBuffer buffer) throws IOException {
		this.minorVersion = Short.toUnsignedInt(buffer.decodeI16());
		this.majorVersion = Short.toUnsignedInt(buffer.decodeI16());
		decodeConstantPool(buffer);
		this.accessFlags = Short.toUnsignedInt(buffer.decodeI16());

		int thisClass = Short.toUnsignedInt(buffer.decodeI16());
		ClassConstant thisClassConstant = resolveConstant(thisClass, ClassConstant.class);

		this.thisClassNameHolder.set(ClassName.fromConstant(thisClassConstant));

		int superClass = Short.toUnsignedInt(buffer.decodeI16());
		ClassConstant superClassConstant = (superClass != 0 ? resolveConstant(superClass, ClassConstant.class) : null);

		this.superClassNameHolder.set(ClassName.fromConstant(superClassConstant));
		decodeInterfaces(buffer);
	}

	private void decodeConstantPool(MCDInputBuffer buffer) throws IOException {
		int count = Short.toUnsignedInt(buffer.decodeI16());

		if (count == 0) {
			throw new IOException("Invalid constant_pool_count: " + count);
		}

		int index = 1;

		while (index < count) {
			int nextIndex = index + 1;
			byte tag = buffer.decodeI8();
			Constant constant;

			switch (tag) {
			// CONSTANT_Utf8
			case Utf8Constant.TAG:
				constant = decodeUtf8Constant(buffer);
				break;
			// CONSTANT_Integer
			case IntegerConstant.TAG:
				constant = decodeIntegerConstant(buffer);
				break;
			// CONSTANT_Float
			case FloatConstant.TAG:
				constant = decodeFloatConstant(buffer);
				break;
			// CONSTANT_Long
			case LongConstant.TAG:
				constant = decodeLongConstant(buffer);
				nextIndex++;
				break;
			// CONSTANT_Double
			case DoubleConstant.TAG:
				constant = decodeDoubleConstant(buffer);
				nextIndex++;
				break;
			// CONSTANT_Class
			case ClassConstant.TAG:
				constant = decodeClassConstant(buffer);
				break;
			// CONSTANT_String
			case StringConstant.TAG:
				constant = decodeStringConstant(buffer);
				break;
			// CONSTANT_Fieldref
			case FieldRefConstant.TAG:
				constant = decodeFieldRefConstant(buffer);
				break;
			// CONSTANT_Methodref
			case MethodRefConstant.TAG:
				constant = decodeMethodRefConstant(buffer);
				break;
			// CONSTANT_InterfaceMethodref
			case InterfaceMethodRefConstant.TAG:
				constant = decodeInterfaceMethodRefConstant(buffer);
				break;
			// CONSTANT_NameAndType
			case NameAndTypeConstant.TAG:
				constant = decodeNameAndTypeConstant(buffer);
				break;
			// CONSTANT_MethodHandle
			case MethodHandleConstant.TAG:
				constant = decodeMethodHandleConstant(buffer);
				break;
			// CONSTANT_MethodType
			case MethodTypeConstant.TAG:
				constant = decodeMethodTypeConstant(buffer);
				break;
			// CONSTANT_Dynamic
			case DynamicConstant.TAG:
				constant = decodeDynamicConstant(buffer);
				break;
			// CONSTANT_InvokeDynamic
			case InvokeDynamicConstant.TAG:
				constant = decodeInvokeDynamicConstant(buffer);
				break;
			// CONSTANT_Module
			case ModuleConstant.TAG:
				constant = decodeModuleConstant(buffer);
				break;
			// CONSTANT_Package
			case PackageConstant.TAG:
				constant = decodePackageConstant(buffer);
				break;
			default:
				throw new IOException("Unrecognized constant tag: " + tag);
			}
			this.constantPool.put(index, constant);
			index = nextIndex;
		}
	}

	private Utf8Constant decodeUtf8Constant(MCDInputBuffer buffer) throws IOException {
		int length = Short.toUnsignedInt(buffer.decodeI16());
		String string = StandardCharsets.UTF_8.decode(buffer.decodeI8Array(length)).toString();

		return new Utf8Constant(this, string);
	}

	private IntegerConstant decodeIntegerConstant(MCDInputBuffer buffer) throws IOException {
		int value = buffer.decodeI32();

		return new IntegerConstant(this, value);
	}

	private FloatConstant decodeFloatConstant(MCDInputBuffer buffer) throws IOException {
		float value = buffer.decodeF32();

		return new FloatConstant(this, value);
	}

	private LongConstant decodeLongConstant(MCDInputBuffer buffer) throws IOException {
		long value = buffer.decodeI64();

		return new LongConstant(this, value);
	}

	private DoubleConstant decodeDoubleConstant(MCDInputBuffer buffer) throws IOException {
		double value = buffer.decodeF64();

		return new DoubleConstant(this, value);
	}

	private ClassConstant decodeClassConstant(MCDInputBuffer buffer) throws IOException {
		int nameIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new ClassConstant(this, nameIndex);
	}

	private StringConstant decodeStringConstant(MCDInputBuffer buffer) throws IOException {
		int stringIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new StringConstant(this, stringIndex);
	}

	private FieldRefConstant decodeFieldRefConstant(MCDInputBuffer buffer) throws IOException {
		int classIndex = Short.toUnsignedInt(buffer.decodeI16());
		int nameAndTypeIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new FieldRefConstant(this, classIndex, nameAndTypeIndex);
	}

	private MethodRefConstant decodeMethodRefConstant(MCDInputBuffer buffer) throws IOException {
		int classIndex = Short.toUnsignedInt(buffer.decodeI16());
		int nameAndTypeIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new MethodRefConstant(this, classIndex, nameAndTypeIndex);
	}

	private InterfaceMethodRefConstant decodeInterfaceMethodRefConstant(MCDInputBuffer buffer) throws IOException {
		int classIndex = Short.toUnsignedInt(buffer.decodeI16());
		int nameAndTypeIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new InterfaceMethodRefConstant(this, classIndex, nameAndTypeIndex);
	}

	private NameAndTypeConstant decodeNameAndTypeConstant(MCDInputBuffer buffer) throws IOException {
		int nameIndex = Short.toUnsignedInt(buffer.decodeI16());
		int descriptorIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new NameAndTypeConstant(this, nameIndex, descriptorIndex);
	}

	private MethodHandleConstant decodeMethodHandleConstant(MCDInputBuffer buffer) throws IOException {
		int referenceKindValue = Byte.toUnsignedInt(buffer.decodeI8());
		int referenceIndex = Short.toUnsignedInt(buffer.decodeI16());
		ReferenceKind referenceKind;

		switch (referenceKindValue) {
		case 1:
			referenceKind = ReferenceKind.REF_getField;
			break;
		case 2:
			referenceKind = ReferenceKind.REF_getStatic;
			break;
		case 3:
			referenceKind = ReferenceKind.REF_putField;
			break;
		case 4:
			referenceKind = ReferenceKind.REF_putStatic;
			break;
		case 5:
			referenceKind = ReferenceKind.REF_invokeVirtual;
			break;
		case 6:
			referenceKind = ReferenceKind.REF_invokeStatic;
			break;
		case 7:
			referenceKind = ReferenceKind.REF_invokeSpecial;
			break;
		case 8:
			referenceKind = ReferenceKind.REF_newInvokeSpecial;
			break;
		case 9:
			referenceKind = ReferenceKind.REF_invokeInterface;
			break;
		default:
			throw new IOException("Unrecognized reference kind: " + referenceKindValue);
		}
		return new MethodHandleConstant(this, referenceKind, referenceIndex);
	}

	private MethodTypeConstant decodeMethodTypeConstant(MCDInputBuffer buffer) throws IOException {
		int descriptorIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new MethodTypeConstant(this, descriptorIndex);
	}

	private DynamicConstant decodeDynamicConstant(MCDInputBuffer buffer) throws IOException {
		int bootstrapMethodAttrIndex = Short.toUnsignedInt(buffer.decodeI16());
		int nameAndTypeIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new DynamicConstant(this, bootstrapMethodAttrIndex, nameAndTypeIndex);
	}

	private InvokeDynamicConstant decodeInvokeDynamicConstant(MCDInputBuffer buffer) throws IOException {
		int bootstrapMethodAttrIndex = Short.toUnsignedInt(buffer.decodeI16());
		int nameAndTypeIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new InvokeDynamicConstant(this, bootstrapMethodAttrIndex, nameAndTypeIndex);
	}

	private ModuleConstant decodeModuleConstant(MCDInputBuffer buffer) throws IOException {
		int nameIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new ModuleConstant(this, nameIndex);
	}

	private PackageConstant decodePackageConstant(MCDInputBuffer buffer) throws IOException {
		int nameIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new PackageConstant(this, nameIndex);
	}

	private void decodeInterfaces(MCDInputBuffer buffer) throws IOException {
		int interfacesCount = Short.toUnsignedInt(buffer.decodeI16());
		ByteBuffer interfacesIndices = buffer.decodeI16Array(interfacesCount);
		List<ClassName> interfaces = new ArrayList<>(interfacesCount);

		while (interfacesIndices.hasRemaining()) {
			int interfaceIndex = Short.toUnsignedInt(interfacesIndices.getShort());

			interfaces.add(ClassName.fromConstant(resolveConstant(interfaceIndex, ClassConstant.class)));
		}
		this.interfacesHolder.set(Collections.unmodifiableList(interfaces));
	}

	private void decodeFields(MCDInputBuffer buffer) throws IOException {
		int fieldsCount = Short.toUnsignedInt(buffer.decodeI16());
		List<FieldInfo> fields = new ArrayList<>(fieldsCount);

		for (int fieldsIndex = 0; fieldsIndex < fieldsCount; fieldsIndex++) {
			int fieldAccessFlags = Short.toUnsignedInt(buffer.decodeI16());
			int fieldNameIndex = Short.toUnsignedInt(buffer.decodeI16());
			int fieldDescriptorIndex = Short.toUnsignedInt(buffer.decodeI16());
			List<Attribute> fieldAttributes = decodeAttributes(buffer);

			fields.add(new Field(this, fieldAccessFlags, fieldNameIndex, fieldDescriptorIndex, fieldAttributes));
		}
		this.fieldsHolder.set(Collections.unmodifiableList(fields));
	}

	private void decodeMethods(MCDInputBuffer buffer) throws IOException {
		int methodsCount = Short.toUnsignedInt(buffer.decodeI16());
		List<MethodInfo> methods = new ArrayList<>(methodsCount);

		for (int methodsIndex = 0; methodsIndex < methodsCount; methodsIndex++) {
			int methodAccessFlags = Short.toUnsignedInt(buffer.decodeI16());
			int methodNameIndex = Short.toUnsignedInt(buffer.decodeI16());
			int methodDescriptorIndex = Short.toUnsignedInt(buffer.decodeI16());
			List<Attribute> methodAttributes = decodeAttributes(buffer);

			methods.add(new Method(this, methodAccessFlags, methodNameIndex, methodDescriptorIndex, methodAttributes));
		}
		this.methodsHolder.set(Collections.unmodifiableList(methods));
	}

	private void decodeClassAttributes(MCDInputBuffer buffer) throws IOException {
		this.attributesHolder.set(Collections.unmodifiableList(decodeAttributes(buffer)));
	}

	private List<Attribute> decodeAttributes(MCDInputBuffer buffer) throws IOException {
		int attributesCount = Short.toUnsignedInt(buffer.decodeI16());
		List<Attribute> attributes = new ArrayList<>(attributesCount);

		for (int attributesIndex = 0; attributesIndex < attributesCount; attributesIndex++) {
			decodeAttribute(attributes, buffer);
		}
		return attributes;
	}

	private void decodeAttribute(List<Attribute> attributes, MCDInputBuffer buffer) throws IOException {
		int nameIndex = Short.toUnsignedInt(buffer.decodeI16());
		int length = buffer.decodeI32();
		String attributeName = resolveConstant(nameIndex, Utf8Constant.class).getValue();

		switch (attributeName) {
		case SourceFileAttribute.NAME:
			attributes.add(decodeSourceFileAttribute(buffer));
			break;
		case ConstantValueAttribute.NAME:
			attributes.add(decodeConstantValueAttribute(buffer));
			break;
		case RuntimeVisibleAnnotationsAttribute.NAME:
			attributes.add(decodeRuntimeVisibleAnnotationsAttribute(buffer));
			break;
		case RuntimeInvisibleAnnotationsAttribute.NAME:
			attributes.add(decodeRuntimeInvisibleAnnotationsAttribute(buffer));
			break;
		case RuntimeVisibleTypeAnnotationsAttribute.NAME:
			attributes.add(decodeRuntimeVisibleTypeAnnotationsAttribute(buffer));
			break;
		case RuntimeInvisibleTypeAnnotationsAttribute.NAME:
			attributes.add(decodeRuntimeInvisibleTypeAnnotationsAttribute(buffer));
			break;
		case SignatureAttribute.NAME:
			attributes.add(decodeSignatureAttribute(buffer));
			break;
		case ExceptionsAttribute.NAME:
			attributes.add(decodeExceptionsAttribute(buffer));
			break;
		case CodeAttribute.NAME:
			attributes.add(new CodeAttribute(this, buffer.slice(Integer.toUnsignedLong(length))));
			break;
		default:
			buffer.skip(length);

			LOG.debug("Skipping attribute ''{0}''", attributeName);
		}
	}

	private SourceFileAttribute decodeSourceFileAttribute(MCDInputBuffer buffer) throws IOException {
		int sourceFileIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new SourceFileAttribute(this, sourceFileIndex);
	}

	private ConstantValueAttribute decodeConstantValueAttribute(MCDInputBuffer buffer) throws IOException {
		int constantValueIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new ConstantValueAttribute(this, constantValueIndex);
	}

	private RuntimeVisibleAnnotationsAttribute decodeRuntimeVisibleAnnotationsAttribute(MCDInputBuffer buffer)
			throws IOException {
		List<Annotation> annotations = decodeAnnotations(buffer);

		return new RuntimeVisibleAnnotationsAttribute(this, annotations);
	}

	private RuntimeInvisibleAnnotationsAttribute decodeRuntimeInvisibleAnnotationsAttribute(MCDInputBuffer buffer)
			throws IOException {
		List<Annotation> annotations = decodeAnnotations(buffer);

		return new RuntimeInvisibleAnnotationsAttribute(this, annotations);
	}

	private RuntimeVisibleTypeAnnotationsAttribute decodeRuntimeVisibleTypeAnnotationsAttribute(MCDInputBuffer buffer)
			throws IOException {
		List<TypeAnnotation> annotations = decodeTypeAnnotations(buffer);

		return new RuntimeVisibleTypeAnnotationsAttribute(this, annotations);
	}

	private RuntimeInvisibleTypeAnnotationsAttribute decodeRuntimeInvisibleTypeAnnotationsAttribute(
			MCDInputBuffer buffer) throws IOException {
		List<TypeAnnotation> annotations = decodeTypeAnnotations(buffer);

		return new RuntimeInvisibleTypeAnnotationsAttribute(this, annotations);
	}

	private List<Annotation> decodeAnnotations(MCDInputBuffer buffer) throws IOException {
		int count = Short.toUnsignedInt(buffer.decodeI16());
		List<Annotation> annotations = new LinkedList<>();

		for (int index = 0; index < count; index++) {
			Annotation annotation = decodeAnnotation(buffer);

			annotations.add(annotation);
		}
		return annotations;
	}

	private Annotation decodeAnnotation(MCDInputBuffer buffer) throws IOException {
		int typeIndex = Short.toUnsignedInt(buffer.decodeI16());
		List<AnnotationElement> elements = decodeAnnotationElements(buffer);

		return new Annotation(this, typeIndex, elements);
	}

	private List<TypeAnnotation> decodeTypeAnnotations(MCDInputBuffer buffer) throws IOException {
		int count = Short.toUnsignedInt(buffer.decodeI16());
		List<TypeAnnotation> annotations = new LinkedList<>();

		for (int index = 0; index < count; index++) {
			TypeAnnotation annotation = decodeTypeAnnotation(buffer);

			annotations.add(annotation);
		}
		return annotations;
	}

	private TypeAnnotation decodeTypeAnnotation(MCDInputBuffer buffer) throws IOException {
		TypeAnnotationTarget target = decodeTypeAnnotationTarget(buffer);
		TypeAnnotationPath path = decodeTypeAnnotationPath(buffer);
		int typeIndex = Short.toUnsignedInt(buffer.decodeI16());
		List<AnnotationElement> elements = decodeAnnotationElements(buffer);

		return new TypeAnnotation(this, typeIndex, elements, target, path);
	}

	private TypeAnnotationTarget decodeTypeAnnotationTarget(MCDInputBuffer buffer) throws IOException {
		int targetType = Byte.toUnsignedInt(buffer.decodeI8());
		TypeAnnotationTarget target;

		switch (targetType) {
		case 0x00:
		case 0x01:
			target = decodeTypeParameterTarget(targetType, buffer);
			break;
		case 0x10:
			target = decodeSupertypeTarget(targetType, buffer);
			break;
		case 0x11:
		case 0x12:
			target = decodeTypeParameterBoundTarget(targetType, buffer);
			break;
		case 0x13:
		case 0x14:
		case 0x15:
			target = new TypeAnnotationTarget.Empty(targetType);
			break;
		case 0x16:
			target = decodeFormalParameterTarget(targetType, buffer);
			break;
		case 0x17:
			target = decodeThrowsTypeTarget(targetType, buffer);
			break;
		case 0x40:
		case 0x41:
			target = decodeLocalvarTarget(targetType, buffer);
			break;
		case 0x42:
			target = decodeCatchTypeTarget(targetType, buffer);
			break;
		case 0x43:
		case 0x44:
		case 0x45:
		case 0x46:
			target = decodeOffsetTarget(targetType, buffer);
			break;
		case 0x47:
		case 0x48:
		case 0x49:
		case 0x4a:
		case 0x4b:
			target = decodeTypeArgumentTarget(targetType, buffer);
			break;
		default:
			throw new IOException("Unrecognized target type: " + targetType);
		}
		return target;
	}

	private TypeAnnotationTarget decodeTypeParameterTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int parameterIndex = Byte.toUnsignedInt(buffer.decodeI8());

		return new TypeAnnotationTarget.TypeParameter(targetType, parameterIndex);
	}

	private TypeAnnotationTarget decodeSupertypeTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int supertypeIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new TypeAnnotationTarget.Supertype(targetType, supertypeIndex);
	}

	private TypeAnnotationTarget decodeTypeParameterBoundTarget(int targetType, MCDInputBuffer buffer)
			throws IOException {
		int parameterIndex = Byte.toUnsignedInt(buffer.decodeI8());
		int boundIndex = Byte.toUnsignedInt(buffer.decodeI8());

		return new TypeAnnotationTarget.TypeParameterBound(targetType, parameterIndex, boundIndex);
	}

	private TypeAnnotationTarget decodeFormalParameterTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int parameterIndex = Byte.toUnsignedInt(buffer.decodeI8());

		return new TypeAnnotationTarget.FormalParameter(targetType, parameterIndex);
	}

	private TypeAnnotationTarget decodeThrowsTypeTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int throwsIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new TypeAnnotationTarget.ThrowsType(targetType, throwsIndex);
	}

	private TypeAnnotationTarget decodeLocalvarTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int tableLength = Short.toUnsignedInt(buffer.decodeI16());
		short[] table = MCDInputBuffer.toI16Array(buffer.decodeI16Array(tableLength));

		return new TypeAnnotationTarget.Localvar(targetType, table);
	}

	private TypeAnnotationTarget decodeCatchTypeTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int exceptionIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new TypeAnnotationTarget.CatchType(targetType, exceptionIndex);
	}

	private TypeAnnotationTarget decodeOffsetTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int offset = Short.toUnsignedInt(buffer.decodeI16());

		return new TypeAnnotationTarget.Offset(targetType, offset);
	}

	private TypeAnnotationTarget decodeTypeArgumentTarget(int targetType, MCDInputBuffer buffer) throws IOException {
		int offset = Short.toUnsignedInt(buffer.decodeI16());
		int argumentIndex = Byte.toUnsignedInt(buffer.decodeI8());

		return new TypeAnnotationTarget.TypeArgument(targetType, offset, argumentIndex);
	}

	private TypeAnnotationPath decodeTypeAnnotationPath(MCDInputBuffer buffer) throws IOException {
		int pathLength = Byte.toUnsignedInt(buffer.decodeI8()) * 2;
		byte[] path = MCDInputBuffer.toI8Array(buffer.decodeI8Array(pathLength));

		return new TypeAnnotationPath(path);
	}

	private List<AnnotationElement> decodeAnnotationElements(MCDInputBuffer buffer) throws IOException {
		int elementsCount = Short.toUnsignedInt(buffer.decodeI16());
		List<AnnotationElement> elements = new ArrayList<>(elementsCount);

		for (int elementIndex = 0; elementIndex < elementsCount; elementIndex++) {
			int elementNameIndex = Short.toUnsignedInt(buffer.decodeI16());
			AnnotationElementValue elementValue = decodeAnnotationElementValue(buffer);

			elements.add(new AnnotationElement(this, elementNameIndex, elementValue));
		}
		return elements;
	}

	private AnnotationElementValue decodeAnnotationElementValue(MCDInputBuffer buffer) throws IOException {
		int elementTag = Byte.toUnsignedInt(buffer.decodeI8());
		AnnotationElementValue elementValue;

		switch (elementTag) {
		case ByteAnnotationElement.TAG:
			elementValue = new ByteAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case CharAnnotationElement.TAG:
			elementValue = new CharAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case DoubleAnnotationElement.TAG:
			elementValue = new DoubleAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case FloatAnnotationElement.TAG:
			elementValue = new FloatAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case IntAnnotationElement.TAG:
			elementValue = new IntAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case LongAnnotationElement.TAG:
			elementValue = new LongAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case ShortAnnotationElement.TAG:
			elementValue = new ShortAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case BooleanAnnotationElement.TAG:
			elementValue = new BooleanAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case StringAnnotationElement.TAG:
			elementValue = new StringAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case EnumAnnotationElement.TAG:
			elementValue = new EnumAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()),
					Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case ClassAnnotationElement.TAG:
			elementValue = new ClassAnnotationElement(this, Short.toUnsignedInt(buffer.decodeI16()));
			break;
		case AnnotationAnnotationElement.TAG:
			elementValue = new AnnotationAnnotationElement(this, decodeAnnotation(buffer));
			break;
		case ArrayAnnotationElement.TAG:
			elementValue = new ArrayAnnotationElement(this, decodeAnnotationElementValues(buffer));
			break;
		default:
			throw new IOException("Unrecognized annotation element tag: " + elementTag);
		}
		return elementValue;
	}

	private List<AnnotationElementValue> decodeAnnotationElementValues(MCDInputBuffer buffer) throws IOException {
		int elementValuesCount = Short.toUnsignedInt(buffer.decodeI16());
		List<AnnotationElementValue> elementValues = new ArrayList<>();

		for (int elementValueIndex = 0; elementValueIndex < elementValuesCount; elementValueIndex++) {
			elementValues.add(decodeAnnotationElementValue(buffer));
		}
		return elementValues;
	}

	private SignatureAttribute decodeSignatureAttribute(MCDInputBuffer buffer) throws IOException {
		int signatureIndex = Short.toUnsignedInt(buffer.decodeI16());

		return new SignatureAttribute(this, signatureIndex);
	}

	private ExceptionsAttribute decodeExceptionsAttribute(MCDInputBuffer buffer) throws IOException {
		int exceptionsCount = Short.toUnsignedInt(buffer.decodeI16());
		ByteBuffer exceptionsBuffer = buffer.decodeI16Array(exceptionsCount);
		int[] exceptions = new int[exceptionsCount];

		for (int exceptionIndex = 0; exceptionIndex < exceptionsCount; exceptionIndex++) {
			exceptions[exceptionIndex] = Short.toUnsignedInt(exceptionsBuffer.getShort());
		}
		return new ExceptionsAttribute(this, exceptions);
	}

	@Override
	public int majorVersion() {
		return this.majorVersion;
	}

	@Override
	public int minorVersion() {
		return this.minorVersion;
	}

	@Override
	public <T extends Constant> T resolveConstant(int index, Class<T> type) throws IOException {
		Constant constant = this.constantPool.get(index);

		if (constant == null) {
			throw new IOException("Invalid constant index: " + index);
		}
		if (!type.isAssignableFrom(constant.getClass())) {
			throw new IOException("Constant type mismatch: " + constant.getClass());
		}
		return type.cast(constant);
	}

	@Override
	public String resolveRuntimeSymbol(int index) {
		Constant constant = this.constantPool.get(index);
		String symbol;

		if (constant != null) {
			try {
				symbol = constant.resolveSymbol();
			} catch (IOException e) {
				LOG.error(e, "Failed to resolve symbol of {0} constant", constant.getClass().getName());

				symbol = "<could not be resolved>";
			}
		} else {
			symbol = "<run-time constant>";
		}
		return symbol;
	}

	@Override
	public int accessFlags() {
		return this.accessFlags;
	}

	@Override
	public ClassName thisClass() {
		return this.thisClassNameHolder.get();
	}

	@Override
	public ClassName superClass() {
		return this.superClassNameHolder.get();
	}

	@Override
	public List<ClassName> interfaces() {
		return this.interfacesHolder.get();
	}

	@Override
	public List<FieldInfo> fields() {
		return this.fieldsHolder.get();
	}

	@Override
	public List<MethodInfo> methods() {
		return this.methodsHolder.get();
	}

	@Override
	public List<Attribute> attributes() {
		return this.attributesHolder.get();
	}

}
