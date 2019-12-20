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
package de.carne.mcd.jvm.decode;

import java.io.IOException;

import de.carne.mcd.jvm.ClassContext;
import de.carne.mcd.jvm.ClassInfo;
import de.carne.mcd.jvm.ClassPrinter;

/**
 * Deprecated attribute: "Deprecated"
 */
public class DeprecatedAttribute extends Attribute {

	/**
	 * Attribute name: "Deprecated"
	 */
	public static final String NAME = "Deprecated";

	DeprecatedAttribute(ClassInfo classInfo) {
		super(classInfo);
	}

	@Override
	public void print(ClassPrinter out, ClassContext context) throws IOException {
		if (context.isOneOf(ClassContext.CLASS, ClassContext.METHOD)) {
			out.printlnLabel(ClassPrinter.S_DEPRECATED);
		} else {
			out.printLabel(ClassPrinter.S_DEPRECATED).print(" ");
		}
	}

}