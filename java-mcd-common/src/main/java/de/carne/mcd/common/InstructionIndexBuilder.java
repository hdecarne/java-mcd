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
package de.carne.mcd.common;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.carne.boot.logging.Log;

/**
 * Helper class used to create an {@linkplain InstructionIndex} resource file.
 */
public final class InstructionIndexBuilder implements InstructionIndexParameters {

	private static final Log LOG = new Log();

	private static class InstructionEntry {

		private final Instruction instruction;
		private final int instructionSize;

		InstructionEntry(Instruction instruction, int instructionSize) {
			this.instruction = instruction;
			this.instructionSize = instructionSize;
		}

		public Instruction instruction() {
			return this.instruction;
		}

		public int instructionSize() {
			return this.instructionSize;
		}

	}

	private final SortedMap<Opcode, InstructionEntry> instructionTable = new TreeMap<>();
	private int maxOpcodeLength = 0;
	private long totalInstructionSize = 0;

	/**
	 * Adds an {@linkplain Opcode} to {@linkplain Instruction} mapping to the index.
	 *
	 * @param opcode the {@linkplain Opcode} to map from.
	 * @param instruction the {@linkplain Instruction} to map to.
	 * @throws IOException if an I/O error occurs.
	 */
	public void add(Opcode opcode, Instruction instruction) throws IOException {
		int instructionSize;

		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(bytes)) {
			instruction.store(out);
			out.flush();
			instructionSize = bytes.size();
		}
		this.instructionTable.put(opcode, new InstructionEntry(instruction, instructionSize));
		this.maxOpcodeLength = Math.max(this.maxOpcodeLength, opcode.length());
		this.totalInstructionSize += instructionSize;
	}

	/**
	 * Stores the current state of this instance to a {@linkplain DataOutput}.
	 * <p>
	 * The resulting output can accessed via {@linkplain InstructionIndex#open(InstructionFactory, java.net.URL)}.
	 * </p>
	 *
	 * @param out the {@linkplain DataOutput} write to.
	 * @return the size of the stored index.
	 * @throws IOException if an I/O error occurs.
	 */
	public long store(DataOutput out) throws IOException {
		LOG.info("Storing instruction index...");

		long totalIndexSize = 0;

		out.writeInt(parameters());
		totalIndexSize += 4;

		int entryCount = this.instructionTable.size();
		int opcodeBytes = opcodeBytes();
		int positionBytes = positionBytes();

		LOG.debug(" Index parameters: {0}/{1}/{2}", entryCount, opcodeBytes, positionBytes);

		long nextInstructionPosition = 0;

		for (Map.Entry<Opcode, InstructionEntry> entry : this.instructionTable.entrySet()) {
			Opcode opcode = entry.getKey();

			LOG.debug(" {0} -> {1}", opcode, nextInstructionPosition);

			InstructionEntry instructionEntry = entry.getValue();

			out.write(opcode.encode(opcodeBytes));
			totalIndexSize += opcodeBytes;
			out.write(getEncodedPosition(nextInstructionPosition, positionBytes));
			totalIndexSize += positionBytes;
			nextInstructionPosition += instructionEntry.instructionSize();
		}
		for (InstructionEntry instructionEntry : this.instructionTable.values()) {
			instructionEntry.instruction().store(out);
			totalIndexSize += instructionEntry.instructionSize();
		}
		return totalIndexSize;
	}

	private byte[] getEncodedPosition(long position, int positionBytes) {
		byte[] encoded = new byte[positionBytes];
		long shift = position;

		for (int encodeIndex = 0; encodeIndex < positionBytes; encodeIndex++) {
			encoded[encodeIndex] = (byte) (shift & 0xff);
			shift >>= 8;
		}
		return encoded;
	}

	@Override
	public int entryCount() {
		return this.instructionTable.size();
	}

	@Override
	public int opcodeBytes() {
		return (((1 + this.maxOpcodeLength) + 1) & ~0x1);
	}

	@Override
	public int positionBytes() {
		int positionBytes;

		if (this.totalInstructionSize <= 0x10000) {
			positionBytes = 2;
		} else if (this.totalInstructionSize <= 0x100000000l) {
			positionBytes = 4;
		} else {
			positionBytes = 8;
		}
		return positionBytes;
	}

}
