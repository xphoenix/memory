package org.xphoenix.memory.core;

import static org.junit.Assert.assertEquals;

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
import org.xphoenix.memory.core.MemoryAccessUnsafeImpl;

@RunWith(Parameterized.class)
public class MemoryAccessUnsafeImplTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(MemoryAccessUnsafeImplTest.class);

	private final static Random RANDOM = new Random(0); 

	private final static int TEST_REPETITION_COUNT = 100;

	@Parameterized.Parameters
	public static Collection memoryAccessImpls() {
		int minSize = 128;
		int maxSize = 65535;
		return Arrays.asList(new Object[][] { 
				{ MemoryAccessUnsafeImpl.wrap(BoundsChecker.NO_OP, new byte[minSize + RANDOM.nextInt(maxSize - minSize + 1)]) }, 
				{ MemoryAccessUnsafeImpl.wrap(BoundsChecker.REAL, new byte[minSize + RANDOM.nextInt(maxSize - minSize + 1)]) }, 

				{ MemoryAccessUnsafeImpl.wrap(BoundsChecker.NO_OP, ByteOrderConvertor.NO_OP, ByteBuffer.allocateDirect(minSize + RANDOM.nextInt(maxSize - minSize + 1))) }, 
				{ MemoryAccessUnsafeImpl.wrap(BoundsChecker.REAL, ByteOrderConvertor.NO_OP, ByteBuffer.allocateDirect(minSize + RANDOM.nextInt(maxSize - minSize + 1))) }, 
				
				{ MemoryAccessUnsafeImpl.wrap(BoundsChecker.NO_OP, ByteOrderConvertor.SWAP, ByteBuffer.allocateDirect(minSize + RANDOM.nextInt(maxSize - minSize + 1))) }, 
				{ MemoryAccessUnsafeImpl.wrap(BoundsChecker.REAL, ByteOrderConvertor.SWAP, ByteBuffer.allocateDirect(minSize + RANDOM.nextInt(maxSize - minSize + 1))) }, 
		});
   }
	
	private final int size;
	
	private final @NotNull MemoryAccessW memory;
	
	public MemoryAccessUnsafeImplTest(@NotNull MemoryAccessW memory) {
		this.memory = memory;
		this.size = (int)memory.size();
		
		LOG.info("Test round: memory={}", memory);
	}
	
	@Test
	public void testDouble() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			double value = RANDOM.nextDouble();
			long position = RANDOM.nextInt(size - 7);
			
			memory.putDouble(position, value);
			assertEquals("Double#"+i, value, memory.getDouble(position), 0.000000000001);
		}
	}

	@Test
	public void testFloat() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			float value = RANDOM.nextFloat();
			long position = RANDOM.nextInt(size - 7);
			
			memory.putFloat(position, value);
			assertEquals("Float#"+i, value, memory.getFloat(position), 0.000000000001);
		}
	}

	@Test
	public void testLong() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			long value = RANDOM.nextLong();
			long position = RANDOM.nextInt(size - 7);
			
			memory.putLong(position, value);
			assertEquals("Long#"+i, value, memory.getLong(position));
		}
	}

	@Test
	public void testInt() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			int value = RANDOM.nextInt();
			long position = RANDOM.nextInt(size - 3);
			
			memory.putInt(position, value);
			assertEquals("Int#"+i, value, memory.getInt(position));
		}
	}

	@Test
	public void testShort() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			short value = (short)(RANDOM.nextInt(2*Short.MAX_VALUE+2) - 2 * Short.MAX_VALUE);
			long position = RANDOM.nextInt(size - 1);
			
			memory.putShort(position, value);
			assertEquals("Short#"+i, value, memory.getShort(position));
		}
	}

	@Test
	public void testChar() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			char value = (char)(RANDOM.nextInt(Character.MAX_VALUE + 1));
			long position = RANDOM.nextInt(size - 1);
			
			memory.putChar(position, value);
			assertEquals("Char#"+i, value, memory.getChar(position));
		}
	}

	@Test
	public void testByte() {
		for (int i=0; i < TEST_REPETITION_COUNT; i++) {
			byte value = (byte)(RANDOM.nextInt(2 *Byte.MAX_VALUE + 2) - 2 * Byte.MAX_VALUE);
			long position = RANDOM.nextInt(size - 1);
			
			memory.putByte(position, value);
			assertEquals("Byte#"+i, value, memory.getByte(position));
		}
	}
}
