package org.xphoenix.memory.core;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xphoenix.memory.core.BoundsChecker;
import org.xphoenix.memory.core.ByteOrderConvertor;
import org.xphoenix.memory.core.MemoryAccessW;
import org.xphoenix.memory.core.MemoryAccessAggregationImpl;
import org.xphoenix.memory.core.MemoryAccessUnsafeImpl;

@RunWith(Parameterized.class)
public class MemoryAccessAggregationImplTest {

	private static final int SEGMENT_SIZE = 10;
	
	@Parameterized.Parameters
	public static Collection memoryAccessImpls() {
		return Arrays.asList(new Object[][] {
				{
					new MemoryAccessAggregationImpl(
						new MemoryAccessW[]{
								MemoryAccessUnsafeImpl.wrap(new byte[SEGMENT_SIZE]),
								MemoryAccessUnsafeImpl.wrap(new byte[SEGMENT_SIZE]),
								null
						},
						0
					)
				},

				{
					new MemoryAccessAggregationImpl(
						new MemoryAccessW[]{
								MemoryAccessUnsafeImpl.wrap(ByteBuffer.allocateDirect(SEGMENT_SIZE)),
								MemoryAccessUnsafeImpl.wrap(ByteBuffer.allocateDirect(SEGMENT_SIZE)),
								null
						},
						0
					)
				},
				
				{
					new MemoryAccessAggregationImpl(
						new MemoryAccessW[]{
								MemoryAccessUnsafeImpl.wrap(BoundsChecker.REAL, ByteOrderConvertor.NO_OP, new byte[SEGMENT_SIZE]),
								MemoryAccessUnsafeImpl.wrap(BoundsChecker.REAL, ByteOrderConvertor.NO_OP, ByteBuffer.allocateDirect(SEGMENT_SIZE)),
								null
						},
						0
					)
				},
		});
	}
	
	private final @NotNull MemoryAccessW memory;
	
	public MemoryAccessAggregationImplTest(@NotNull MemoryAccessW memory) {
		this.memory = memory;
	}


	@Test
	public void testSize() {
		assertEquals("Size", SEGMENT_SIZE * 2, memory.size());
	}

	@Test(expected=RuntimeException.class)
	public void testDifferentSize() {
		new MemoryAccessAggregationImpl(
				new MemoryAccessW[]{
						MemoryAccessUnsafeImpl.wrap(new byte[SEGMENT_SIZE]),
						MemoryAccessUnsafeImpl.wrap(new byte[SEGMENT_SIZE/2]),
						null
				},
				0
		);
	}
	
	@Test
	public void testByte() {

		for (int i=0; i < SEGMENT_SIZE; i++) {
			memory.putByte(i, (byte)0x12);
			assertEquals("getByte#"+i, Integer.toHexString((byte)0x12), Integer.toHexString(memory.getByte(i)));
		}
	}

	@Test
	public void testChar() {

		for (int i=0; i < SEGMENT_SIZE; i++) {
			memory.putChar(i, (char)0x1122);
			assertEquals("getChar#"+i, Integer.toHexString((char)0x1122), Integer.toHexString(memory.getChar(i)));
		}
	}

	@Test
	public void testShort() {

		for (int i=0; i < SEGMENT_SIZE; i++) {
			memory.putShort(i, (short)0x1122);
			assertEquals("getShort#"+i, Integer.toHexString((short)0x1122), Integer.toHexString(memory.getShort(i)));
		}
	}

	@Test
	public void testInt() {

		for (int i=0; i < SEGMENT_SIZE; i++) {
			memory.putInt(i, 0x11223344);
			assertEquals("getInt#"+i, Integer.toHexString(0x11223344), Integer.toHexString(memory.getInt(i)));
		}
	}

	@Test
	public void testLong() {

		for (int i=0; i < SEGMENT_SIZE; i++) {
			memory.putLong(i, 0x1122334455667788L);
			assertEquals("getLong#"+i, Long.toHexString(0x1122334455667788L), Long.toHexString(memory.getLong(i)));
		}
	}
	
	@Test
	public void testPutBuffer() {
		byte[] buf = new byte[SEGMENT_SIZE *2];
		
		Random r = new Random(0);
		r.nextBytes(buf);
		
		memory.putBuffer(0, buf, 0, buf.length);

		for (int i=0; i < buf.length; i++) {
			assertEquals("buf["+i+"]", buf[i], memory.getByte(i));
		}
	}
}
