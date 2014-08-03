package org.xphoenix.memory.core;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;
import org.xphoenix.memory.UnsafeUtils;

/**
 * Implementation based on sun.misc.Unsafe
 *
 * Used mostly to wrap ByteBuffer | char arrays
 *
 * @author andrphi
 */
public class MemoryAccessUnsafeImpl implements MemoryAccessW {

	public static final MemoryAccessW wrap (byte[] data) {
		return wrap (BoundsChecker.REAL, data);
	}

	public static final MemoryAccessW wrap (@NotNull final BoundsChecker checker, final byte[] data) {
		return wrap (checker, ByteOrderConvertor.NO_OP, data);
	}

	public static final MemoryAccessW wrap (@NotNull final BoundsChecker checker, @NotNull ByteOrderConvertor convertor, final byte[] data) {
		return new MemoryAccessUnsafeImpl(checker, convertor) {

			private final @NotNull ByteBuffer byteBufferView = ByteBuffer.wrap(data);

			@Override
			public final long size() {
				return data.length;
			}

			@Override
			public ByteBuffer[] toByteBuffer() {
				return new ByteBuffer[]{byteBufferView.duplicate()};
			}

			@Override
			public String toString() {
				return "ByteArrayMemoryAccess [checker="+super.checker.name()
						+", order="+super.order.name()
						+", size="+size()
						+"]";
			}

			@Override
			protected final Object object() {
				return data;
			}

			@Override
			protected final long address(long index) {
				return (index << UnsafeUtils.BYTE_ARRAY_SHIFT) + UnsafeUtils.BYTE_ARRAY_OFFSET;
			}
		};
	}
	/**
	 * Wraps given direct ByteBuffer and provides MemoryAccess for it
	 * internal memory
	 *
	 * <p>
	 * 	Created <code>MemoryAccess</code> has {@link BoundsChecker#NO_OP} and {@link ByteOrderConvertor#NO_OP}
	 * </p>
	 *
	 * @param buf direct ByteBuffer
	 * @return MemoryAccess to modify ByteBuffer off heap memory
	 * @throws IllegalArgumentException if given ByteBuffer is not direct
	 */
	public static final MemoryAccessW wrap (final @NotNull ByteBuffer buf) {
		return wrap (
				BoundsChecker.REAL,
				buf
				);
	}

	public static final MemoryAccessW wrap (@NotNull BoundsChecker checker, final @NotNull ByteBuffer buf) {
		return wrap (
				checker,
				ByteOrderConvertor.toNative(buf.order()),
				buf
				);
	}

	/**
	 * Wraps given direct ByteBuffer and provides MemoryAccess for its
	 * internal memroy
	 *
	 * @param checker {@link MemoryAccessUnsafeImpl#MemoryAccessUnsafeImpl(BoundsChecker, ByteOrderConvertor)}
	 * @param convertor {@link MemoryAccessUnsafeImpl#MemoryAccessUnsafeImpl(BoundsChecker, ByteOrderConvertor)}
	 * @param buf direct ByteBuffer
	 * @return MemoryAccess to modify ByteBuffer off heap memory
	 * @throws IllegalArgumentException if given ByteBuffer is not direct
	 */
	public static final MemoryAccessW wrap (@NotNull BoundsChecker checker, @NotNull ByteOrderConvertor convertor, final @NotNull ByteBuffer buf) {
		if (!buf.isDirect()) {
			throw new IllegalArgumentException("MemoryAccessUnsafeImpl could wrap onlly direct buffers");
		}

		return new MemoryAccessUnsafeImpl(checker, convertor) {
			// Final argument will be translated to a hidden field of that class.
			// That reference protects memory from being free. To make trick works we
			// need a direct access to ByteBuffer and not let JIT to remove it
			private final long base = UnsafeUtils.getMemoryAddress(buf);

			@Override
			public long size() {
				return buf.capacity();
			}

			@Override
			public ByteBuffer[] toByteBuffer() {
				return new ByteBuffer[]{buf.duplicate()};
			}

			@Override
			public String toString() {
				return "DirectByteBufferMemoryAccess [checker="+super.checker.name()
						+", order="+super.order.name()
						+", size="+size()
						+"]";
			}

			@Override
			protected long address(long index) {
				return base + index;
			}

			@Override
			protected Object object() {
				return null;
			}
		};
	}
	/**
	 * Delegate responsible for memory bounds checks
	 * {@code BoundsChecker#NOOP} is used by default
	 */
	protected final @NotNull BoundsChecker checker;

	/**
	 * Delegate responsible for byte order transformation
	 * {@code ByteOrderConvertor#BIG_ENDIAN} is used by default
	 */
	protected final @NotNull ByteOrderConvertor order;

	/**
	 * Creates new MemoryAccess object with given BoundsChecker and
	 * ByteOrderConvertor
	 *
	 * @param checker bounds checker
	 * @param convertor byte order convertor
	 * @param size of memory to provide access to
	 */
	protected MemoryAccessUnsafeImpl(@NotNull BoundsChecker checker, @NotNull ByteOrderConvertor convertor) {
		this.checker = checker;
		this.order = convertor;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#size()
	 */
	@Override
	public long size() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getBoundsChecker()
	 */
	@Override
	public BoundsChecker getBoundsChecker() {
		return checker;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getByteOrderConvertor()
	 */
	@Override
	public ByteOrderConvertor getByteOrderConvertor() {
		return order;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getByte(long)
	 */
	@Override
	public byte getByte(long index) {
		checker.checkBounds(index, size());
		byte value = UnsafeUtils.getByte(object(), address(index));

		return value;
	}

	/* (non-Javadoc)
	 * @see vn.ozzy.storage.api.memory.MemoryAccess#getBuffer(int, byte[], int)
	 */
	@Override
	public void getBuffer(byte[] buffer, int index, int size) {
		checker.checkBounds(index + size, size());
		UnsafeUtils.copyMemory(object(), address(index), buffer, 0, size);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getChar(long)
	 */
	@Override
	public char getChar(long index) {
		checker.checkBounds(index, size());
		char value = UnsafeUtils.getChar(object(), address(index));

		return order.decode(value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getDouble(long)
	 */
	@Override
	public double getDouble(long index) {
		checker.checkBounds(index, size());
		double value = UnsafeUtils.getDouble(object(), address(index));

		return order.decode(value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getFloat(long)
	 */
	@Override
	public float getFloat(long index) {
		checker.checkBounds(index, size());
		float value = UnsafeUtils.getFloat(object(), address(index));

		return order.decode(value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getInt(long)
	 */
	@Override
	public int getInt(long index) {
		checker.checkBounds(index, size());
		int value = UnsafeUtils.getInt(object(), address(index));

		return order.decode(value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getLong(long)
	 */
	@Override
	public long getLong(long index) {
		checker.checkBounds(index, size());
		long value = UnsafeUtils.getLong(object(), address(index));

		return order.decode(value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#getShort(long)
	 */
	@Override
	public short getShort(long index) {
		checker.checkBounds(index, size());
		short value = UnsafeUtils.getShort(object(), address(index));

		return order.decode(value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putBuffer(long, java.nio.ByteBuffer)
	 */
	@Override
	public MemoryAccessW putBuffer(long index, @NotNull ByteBuffer value) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putBuffer(long, byte[], int, int)
	 */
	@Override
	public MemoryAccessW putBuffer(long index, @NotNull byte []value, int offset, int size) {
		checker.checkBounds(Math.max(0, index + size-1), size());
		UnsafeUtils.copyMemory(value, offset, object(), address(index), size);

		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putByte(long, byte)
	 */
	@Override
	public MemoryAccessW putByte(long index, byte value) {
		checker.checkBounds(index, size());
		UnsafeUtils.putByte(object(), address(index), value);

		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putChar(long, char)
	 */
	@Override
	public MemoryAccessW putChar(long index, char value) {
		checker.checkBounds(index, size());
		value = order.decode(value);

		UnsafeUtils.putChar(object(), address(index), value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putDouble(long, double)
	 */
	@Override
	public MemoryAccessW putDouble(long index, double value) {
		checker.checkBounds(index, size());
		value = order.decode(value);

		UnsafeUtils.putDouble(object(), address(index), value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putFloat(long, float)
	 */
	@Override
	public MemoryAccessW putFloat(long index, float value) {
		checker.checkBounds(index, size());
		value = order.decode(value);

		UnsafeUtils.putFloat(object(), address(index), value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putInt(long, int)
	 */
	@Override
	public MemoryAccessW putInt(long index, int value) {
		checker.checkBounds(index, size());
		value = order.decode(value);

		UnsafeUtils.putInt(object(), address(index), value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putLong(long, long)
	 */
	@Override
	public MemoryAccessW putLong(long index, long value) {
		checker.checkBounds(index, size());
		value = order.decode(value);

		UnsafeUtils.putLong(object(), address(index), value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#putShort(long, short)
	 */
	@Override
	public MemoryAccessW putShort(long index, short value) {
		checker.checkBounds(index, size());
		value = order.decode(value);

		UnsafeUtils.putShort(object(), address(index), value);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#compareAndSwap(long, int, int)
	 */
	@Override
	public boolean compareAndSwap(long index, int expected, int value) {
		checker.checkBounds(index, size());
		expected = order.decode(expected);
		value  = order.decode(value);

		return UnsafeUtils.compareAndSwapInt(object(), address(index), expected, value);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.storage.api.memory.MemoryAccess#compareAndSwap(long, long, long)
	 */
	@Override
	public boolean compareAndSwap(long index, long expected, long value) {
		checker.checkBounds(index, size());
		expected = order.decode(expected);
		value  = order.decode(value);

		return UnsafeUtils.compareAndSwapLong(object(), address(index), expected, value);
	}

	/**
	 * Provides mapping from a given byte index to a real memory address
	 *
	 * @param index byte index
	 * @return real memory address
	 */
	protected long address (long index) {
		throw new RuntimeException("You must override that method to implement index to address mapping");
	}

	protected Object object() {
		throw new RuntimeException("You must override that method to implement index to address mapping");
	}

	@Override
	public ByteBuffer[] toByteBuffer() {
		throw new RuntimeException("You must override that method to implement index to address mapping");
	}
}
