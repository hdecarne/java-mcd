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

import de.carne.boot.check.Check;

abstract class AbstractRefConstant extends Constant {

	private final int classIndex;
	private final int nameAndTypeIndex;

	AbstractRefConstant(ClassInfo classInfo, int classIndex, int nameAndTypeIndex) {
		super(classInfo);
		this.classIndex = classIndex;
		this.nameAndTypeIndex = nameAndTypeIndex;
	}

	@Override
	public void print(ClassPrinter out, ClassContext context) throws IOException {
		// Should never be called
		Check.fail();
	}

	@Override
	public String resolveSymbol() throws IOException {
		String classPackage = this.classInfo.thisClass().getPackageName();
		String className = getEffectiveClassName(classPackage);
		NameAndTypeConstant nameAndTypeValue = getNameAndTypeValue();
		String name = nameAndTypeValue.getNameValue();
		String descriptor = nameAndTypeValue.getDescriptorValue();

		return className + "." + name + " " + descriptor;
	}

	private String getEffectiveClassName(String classPackage) throws IOException {
		return ClassName.fromConstant(this.classInfo.resolveConstant(this.classIndex, ClassConstant.class))
				.getEffectiveName(classPackage);
	}

	private NameAndTypeConstant getNameAndTypeValue() throws IOException {
		return this.classInfo.resolveConstant(this.nameAndTypeIndex, NameAndTypeConstant.class);
	}

	@Override
	public String toString() {
		return "#" + this.classIndex + ":" + this.nameAndTypeIndex;
	}

}