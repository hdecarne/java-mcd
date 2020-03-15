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

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

import org.eclipse.jdt.annotation.Nullable;

import de.carne.mcd.common.io.MCDInputBuffer;
import de.carne.mcd.common.io.MCDOutput;
import de.carne.mcd.common.io.MCDOutputBuffer;

/**
 * Base class for all kinds of machine code decoders.
 */
public abstract class MachineCodeDecoder {

	private static final ThreadLocal<@Nullable MachineCodeDecoder> ACTIVE_DECODER_HOLDER = new ThreadLocal<>();

	private final String name;
	private ByteOrder byteOrder;

	protected MachineCodeDecoder(String name, ByteOrder byteOrder) {
		this.name = name;
		this.byteOrder = byteOrder;
	}

	/**
	 * Gets the active {@linkplain MachineCodeDecoder}.
	 *
	 * @return the {@linkplain MachineCodeDecoder} instance associated with the current decode call.
	 * @throws IllegalStateException if called outside a decode call.
	 */
	public static MachineCodeDecoder getDecoder() {
		MachineCodeDecoder activeDecoder = ACTIVE_DECODER_HOLDER.get();

		if (activeDecoder == null) {
			throw new IllegalStateException("Decoder only accessible during decode call");
		}
		return activeDecoder;
	}

	/**
	 * Gets the active {@linkplain MachineCodeDecoder}.
	 *
	 * @param <T> the actual decoder type.
	 * @param decoderType the decoder type to get.
	 * @return the {@linkplain MachineCodeDecoder} instance associated with the current decode call.
	 * @throws IllegalStateException if called outside a decode call.
	 */
	public static <T extends MachineCodeDecoder> T getDecoder(Class<T> decoderType) {
		MachineCodeDecoder activeDecoder = getDecoder();
		Class<?> activeDecoderType = activeDecoder.getClass();

		if (!decoderType.isAssignableFrom(activeDecoderType)) {
			throw new IllegalStateException("Decoder type mismatch: " + activeDecoderType.getName());
		}
		return decoderType.cast(activeDecoder);
	}

	/**
	 * Gets this {@linkplain MachineCodeDecoder} instance's name.
	 *
	 * @return this {@linkplain MachineCodeDecoder} instance's name.
	 */
	public String name() {
		return this.name;
	}

	/**
	 * gets this {@linkplain MachineCodeDecoder} instance's byte order.
	 *
	 * @return this {@linkplain MachineCodeDecoder} instance's byte order.
	 */
	public ByteOrder byteOrder() {
		return this.byteOrder;
	}

	/**
	 * Sets this {@linkplain MachineCodeDecoder} instance's byte order.
	 *
	 * @param byteOrder the {@linkplain ByteOrder} to set.
	 */
	public void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}

	/**
	 * Decodes the given byte channel's data.
	 *
	 * @param in the {@linkplain ReadableByteChannel} to decode from.
	 * @param out the {@linkplain MCDOutput} to decode to.
	 * @throws IOException if an I/O error occurs.
	 */
	public void decode(ReadableByteChannel in, MCDOutput out) throws IOException {
		@Nullable MachineCodeDecoder savedDecoder = ACTIVE_DECODER_HOLDER.get();

		ACTIVE_DECODER_HOLDER.set(this);
		try {
			decode0(new MCDInputBuffer(in, this.byteOrder), new MCDOutputBuffer(out));
		} finally {
			if (savedDecoder != null) {
				ACTIVE_DECODER_HOLDER.set(savedDecoder);
			} else {
				ACTIVE_DECODER_HOLDER.remove();
			}
		}
	}

	protected abstract void decode0(MCDInputBuffer in, MCDOutputBuffer out) throws IOException;

}
