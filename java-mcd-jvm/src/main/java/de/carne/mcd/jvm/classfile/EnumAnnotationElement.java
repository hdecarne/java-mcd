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

import de.carne.mcd.jvm.ClassContext;
import de.carne.mcd.jvm.ClassInfo;
import de.carne.mcd.jvm.ClassPrinter;
import de.carne.mcd.jvm.classfile.descriptor.Descriptor;
import de.carne.mcd.jvm.classfile.descriptor.FieldDescriptor;

class EnumAnnotationElement extends AnnotationElementValue {

	public static final int TAG = 'e';

	private final int nameIndex;
	private final int valueIndex;

	public EnumAnnotationElement(ClassInfo classInfo, int nameIndex, int valueIndex) {
		super(classInfo);
		this.nameIndex = nameIndex;
		this.valueIndex = valueIndex;
	}

	@Override
	public void print(ClassPrinter out, ClassContext context) throws IOException {
		FieldDescriptor name = Descriptor.decodeFieldDescriptor(
				this.classInfo.resolveConstant(this.nameIndex, Utf8Constant.class).getValue(),
				this.classInfo.thisClass().getPackageName());
		String value = this.classInfo.resolveConstant(this.valueIndex, Utf8Constant.class).getValue();

		name.print(out, context);
		out.print(".").printValue(value);
	}

}
