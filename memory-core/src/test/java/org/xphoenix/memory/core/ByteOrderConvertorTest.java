package org.xphoenix.memory.core;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;
import org.xphoenix.memory.core.ByteOrderConvertor;

public class ByteOrderConvertorTest {
	
	private static final byte[] data = new byte[8];
	
	private static final ByteBuffer buf = ByteBuffer.wrap(data);
	
	@Test
	public void testJvmByteOrder() {
		buf.order(ByteOrder.BIG_ENDIAN).putInt(0, 0x01020304);
		assertArrayEquals("BIG_ENDIAN", new byte[]{01,02,03,04,00,00,00,00}, data);

		buf.order(ByteOrder.LITTLE_ENDIAN).putInt(0, 0x01020304);
		assertArrayEquals("LITTLE_ENDIAN", new byte[]{04,03,02,01,00,00,00,00}, data);
	}
	
	@Test
	public void testGetters() {
		if (ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN) {
			assertSame("toNative#1", ByteOrderConvertor.SWAP, ByteOrderConvertor.toNative(ByteOrder.BIG_ENDIAN));
			assertSame("toNative#2", ByteOrderConvertor.NO_OP, ByteOrderConvertor.toNative(ByteOrder.LITTLE_ENDIAN));
		} else {
			assertSame("toNative#1", ByteOrderConvertor.NO_OP, ByteOrderConvertor.toNative(ByteOrder.BIG_ENDIAN));
			assertSame("toNative#2", ByteOrderConvertor.SWAP, ByteOrderConvertor.toNative(ByteOrder.LITTLE_ENDIAN));
		}

		assertSame("toJvm#1", ByteOrderConvertor.NO_OP, ByteOrderConvertor.toJvm(ByteOrder.BIG_ENDIAN));
		assertSame("toJvm#2", ByteOrderConvertor.SWAP, ByteOrderConvertor.toJvm(ByteOrder.LITTLE_ENDIAN));
	}
	
	@Test
	public void testNoOpShort() {
		assertEquals(0x0102, ByteOrderConvertor.NO_OP.decode((short)0x0102));
	}

	@Test
	public void testNoOpChar() {
		assertEquals(0x0102, ByteOrderConvertor.NO_OP.decode((char)0x0102));
	}

	@Test
	public void testNoOpInt() {
		assertEquals(0x01020304, ByteOrderConvertor.NO_OP.decode((int)0x01020304));
	}

	@Test
	public void testNoOpLong() {
		assertEquals(0x0102030405060708L, ByteOrderConvertor.NO_OP.decode(0x0102030405060708L));
	}

	@Test
	public void testNoOpDouble() {
		assertEquals(0x0102030405060708L, ByteOrderConvertor.NO_OP.decode((double)0x0102030405060708L), 0.000000000000000000001);
	}

	@Test
	public void testSwapShort() {
		assertEquals(0x0201, ByteOrderConvertor.SWAP.decode((short)0x0102));
	}

	@Test
	public void testSwapChar() {
		assertEquals(0x0201, ByteOrderConvertor.SWAP.decode((char)0x0102));
	}

	@Test
	public void testSwapInt() {
		assertEquals(0x04030201, ByteOrderConvertor.SWAP.decode((int)0x01020304));
	}

	@Test
	public void testSwapLong() {
		assertEquals(0x0807060504030201L, ByteOrderConvertor.SWAP.decode(0x0102030405060708L));
	}
}
