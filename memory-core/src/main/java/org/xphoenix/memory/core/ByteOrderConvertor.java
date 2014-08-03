package org.xphoenix.memory.core;

import java.nio.ByteOrder;

/**
 * Represents byte convertor from Java standard (network order)
 * to big/little endian.
 * 
 * @author andrphi
 */
public enum ByteOrderConvertor {
	
	NO_OP {

		@Override
		public final char decode(char value) {
			return value;
		}

		@Override
		public final int decode(int value) {
			return value;
		}

		@Override
		public final double decode(double value) {
			return value;
		}

		@Override
		public final float decode(float value) {
			return value;
		}

		@Override
		public final long decode(long value) {
			return value;
		}

		@Override
		public final short decode(short value) {
			return value;
		}
	},
	
	SWAP {
		@Override
		public final char decode(char value) {
			return Character.reverseBytes(value);
		}

		@Override
		public final int decode(int value) {
			return Integer.reverseBytes(value); 
		}

		@Override
		public final double decode(double value) {
			return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToLongBits(value)));
		}

		@Override
		public final float decode(float value) {
			return Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(value)));
		}

		@Override
		public final long decode(long value) {
			return Long.reverseBytes(value);
		}

		@Override
		public final short decode(short value) {
			return Short.reverseBytes(value);
		}
	};
	
	public static ByteOrderConvertor toNative (ByteOrder from) {
		return get (from, ByteOrder.nativeOrder());
	}

	public static ByteOrderConvertor toJvm (ByteOrder from) {
		return get (from, ByteOrder.BIG_ENDIAN);
	}
	
	public static ByteOrderConvertor get (ByteOrder from, ByteOrder to) {
		return from == to ? ByteOrderConvertor.NO_OP : ByteOrderConvertor.SWAP;
	}
	
	public char decode(char value) {
		throw new RuntimeException ("Operationmust be mplemented in child");
	}

	public int decode(int value) {
		throw new RuntimeException ("Operationmust be mplemented in child");
	}

	public double decode(double value) {
		throw new RuntimeException ("Operationmust be mplemented in child");
	}

	public float decode(float value) {
		throw new RuntimeException ("Operationmust be mplemented in child");
	}

	public long decode(long value) {
		throw new RuntimeException ("Operationmust be mplemented in child");
	}

	public short decode(short value) {
		throw new RuntimeException ("Operationmust be mplemented in child");
	}
}
