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
package de.carne.mcd.x86;

import java.io.DataOutput;
import java.io.IOException;

import de.carne.boot.check.Check;
import de.carne.mcd.common.Instruction;
import de.carne.mcd.common.MCDDecodeBuffer;
import de.carne.mcd.common.MCDOutput;
import de.carne.text.HexFormat;

class UnknownX86Instruction implements Instruction {

	private final byte[] opcode;

	UnknownX86Instruction(byte[] opcode, int offset, int length) {
		this.opcode = new byte[length];
		System.arraycopy(opcode, offset, this.opcode, 0, length);
	}

	@Override
	public void save(DataOutput out) throws IOException {
		// Should never be called
		Check.fail();
	}

	@Override
	public void decode(int pc, MCDDecodeBuffer buffer, MCDOutput out) throws IOException {
		out.printKeyword("db");
		for (int opcodeByteIndex = 0; opcodeByteIndex < this.opcode.length; opcodeByteIndex++) {
			out.print(opcodeByteIndex > 0 ? ", " : " ").printValue("0x")
					.printValue(HexFormat.LOWER_CASE.format(this.opcode[opcodeByteIndex]));
		}
		out.println(";");
	}

}