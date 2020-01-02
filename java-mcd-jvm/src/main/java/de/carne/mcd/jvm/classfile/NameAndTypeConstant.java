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

class NameAndTypeConstant extends Constant {

	public static final int TAG = 12;

	private final NameDescriptorIndex nameDescriptorIndex;

	NameAndTypeConstant(ClassInfo classInfo, int nameIndex, int descriptorIndex) {
		super(classInfo);
		this.nameDescriptorIndex = new NameDescriptorIndex(nameIndex, descriptorIndex);
	}

	public String getNameValue() throws IOException {
		return this.classInfo.resolveConstant(this.nameDescriptorIndex.nameIndex(), Utf8Constant.class).getValue();
	}

	public String getDescriptorValue() throws IOException {
		return this.classInfo.resolveConstant(this.nameDescriptorIndex.descriptorIndex(), Utf8Constant.class)
				.getValue();
	}

	@Override
	public void print(ClassPrinter out, ClassContext context) throws IOException {
		// Sould never be called
		Check.fail();
	}

	@Override
	public String toString() {
		return this.nameDescriptorIndex.toString();
	}

}
