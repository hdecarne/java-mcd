/*
 * Copyright (c) 2019 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.mcd.jvm;

import java.io.IOException;

class SignatureAttribute extends Attribute {

	public static final String NAME = "Signature";

	private final int signatureIndex;

	public SignatureAttribute(ClassInfo classInfo, int nameIndex, int signatureIndex) {
		super(classInfo, nameIndex);
		this.signatureIndex = signatureIndex;
	}

	@Override
	public void print(ClassPrinter out) throws IOException {
		String signature = this.classInfo.resolveConstant(this.signatureIndex, Utf8Constant.class).getValue();

	}

	@Override
	public String toString() {
		return "#" + this.signatureIndex;
	}

}