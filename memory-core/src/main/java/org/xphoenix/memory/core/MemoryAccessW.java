package org.xphoenix.memory.core;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;


/**
 * Writable memory chunk access interface
 *
 * @author andrphi
 */
public interface MemoryAccessW extends MemoryAccessR {

	@NotNull
	public MemoryAccessW putBuffer(long index, @NotNull ByteBuffer value);

	@NotNull
	public MemoryAccessW putBuffer(long index, @NotNull byte[] value, int offset, int size);

	@NotNull
	public MemoryAccessW putByte(long index, byte value);

	@NotNull
	public MemoryAccessW putChar(long index, char value);

	@NotNull
	public MemoryAccessW putDouble(long index, double value);

	@NotNull
	public MemoryAccessW putFloat(long index, float value);

	@NotNull
	public MemoryAccessW putInt(long index, int value);

	@NotNull
	public MemoryAccessW putLong(long index, long value);

	@NotNull
	public MemoryAccessW putShort(long index, short value);


	public boolean compareAndSwap (long index, int expected, int value);

	public boolean compareAndSwap (long index, long expected, long value);

	@NotNull
	public ByteBuffer []toByteBuffer();
}
