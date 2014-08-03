package org.xphoenix.memory;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.print.DocFlavor.BYTE_ARRAY;

import org.jetbrains.annotations.NotNull;

import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public final class UnsafeUtils {
	
	private static final Unsafe theUnsafe;
	
	private static final Field memoryAddressInByteBuffer;
	
	public static final long BYTE_ARRAY_OFFSET;

	public static final long BYTE_ARRAY_SHIFT;
	
	static {
		try {
			Field  f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			theUnsafe = (Unsafe) f.get(null);
			
			memoryAddressInByteBuffer = Buffer.class.getDeclaredField("address");
			memoryAddressInByteBuffer.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException ("Failed to initialize UnsafeUtils: ", e);
		} 
		
		// Scale cahecks
		if ((Unsafe.ARRAY_BYTE_INDEX_SCALE & (Unsafe.ARRAY_BYTE_INDEX_SCALE - 1)) != 0) {
            throw new Error("byte[] index scale not a power of two");
        }
		
		BYTE_ARRAY_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        BYTE_ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BYTE_INDEX_SCALE);
	}

	public static final Unsafe getUnsafe() {
		return theUnsafe;
	}

	public static long getMemoryAddress(@NotNull ByteBuffer buf) {
		assert buf.isDirect();
		try {
			return memoryAddressInByteBuffer.getLong(buf);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get ByteBuffer memory address: ", e);
		}
	}

	public static final boolean compareAndSwapInt(Object object, long address, int expected, int value) {
		return theUnsafe.compareAndSwapInt(object, address, expected, value);
	}

	public static final boolean compareAndSwapLong(Object object, long address, long expected, long value) {
		return theUnsafe.compareAndSwapLong(object, address, expected, value);
	}

	public static final boolean getBoolean(long address) {
		return theUnsafe.getBoolean(null, address);
	}

	public static final byte getByte(Object object, long address) {
		return theUnsafe.getByte(object, address);
	}

	public static final char getChar(Object object, long address) {
		return theUnsafe.getChar(object, address);
	}

	public static final double getDouble(Object object, long address) {
		return theUnsafe.getDouble(object, address);
	}

	public static final float getFloat(Object object, long address) {
		return theUnsafe.getFloat(object, address);
	}

	public static final int getInt(Object object, long address) {
		return theUnsafe.getInt(object, address);
	}

	public static final long getLong(Object object, long address) {
		return theUnsafe.getLong(object, address);
	}

	public static final short getShort(Object object, long address) {
		return theUnsafe.getShort(object, address);
	}

	public static final void putBoolean(long address, boolean value) {
		theUnsafe.putBoolean(null, address, value);
	}

	public static final void putByte(Object object, long address, byte value) {
		theUnsafe.putByte(object, address, value);
	}

	public static final void putChar(Object object, long address, char value) {
		theUnsafe.putChar(object, address, value);
	}

	public static final void putDouble(Object object, long address, double value) {
		theUnsafe.putDouble(object, address, value);
	}

	public static final void putFloat(Object object, long address, float value) {
		theUnsafe.putFloat(object, address, value);
	}

	public static final void putInt(Object object, long address, int value) {
		theUnsafe.putInt(object, address, value);
	}

	public static final void putLong(Object object, long address, long value) {
		theUnsafe.putLong(object, address, value);
	}

	public static final void putShort(Object object, long address, short value) {
		theUnsafe.putShort(object, address, value);
	}
	
	public static final void copyMemory(byte[] array, long address, int len) {
		theUnsafe.copyMemory(array, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address, len);
	}

	public static final void copyMemory(byte[] array, int offset, Object dest, long address, int len) {
		theUnsafe.copyMemory(
				array, 
				(offset << UnsafeUtils.BYTE_ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET, 
				dest, 
				address,
				len
		);
	}

	public static final void copyMemory(Object object, long address, byte[] array, int offset, int len) {
		theUnsafe.copyMemory(
				object, 
				address, 
				array, 
				(offset << UnsafeUtils.BYTE_ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET, 
				len
		);
	}
}
