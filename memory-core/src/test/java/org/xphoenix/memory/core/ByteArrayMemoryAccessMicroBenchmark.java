package org.xphoenix.memory.core;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;
import org.xphoenix.memory.core.BoundsChecker;
import org.xphoenix.memory.core.MemoryAccessW;
import org.xphoenix.memory.core.MemoryAccessUnsafeImpl;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;

/**
 * Measure performance of put/get primitive types for 
 * ByteArray and ByteBuffer (direct) MemoryAccess impl
 * 
 * @author andrphi
 */
public class ByteArrayMemoryAccessMicroBenchmark extends Benchmark {

	@Param({"16384", "32768", "65536", "131072"})
	private int size;
	
	@Param
	private BoundsChecker boundsChecker = BoundsChecker.NO_OP;
	
	@Param
	private MemoryAccessFactory factory = MemoryAccessFactory.BYTE_ARRAY;
	
	private MemoryAccessW memory;

	@Override
	public void setUp() throws Exception {
		memory = factory.create(boundsChecker, size);
	}

	@Override
  	public void tearDown() throws Exception {
		memory = null;
	}
	
	public int timeLongPut(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j += 8) {
				dummy += memory.putLong(j, j).hashCode();
			}
		}
		return dummy;
	}

	public int timeIntPut(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j += 4) {
				dummy += memory.putInt(j, j).hashCode();
			}
		}
		return dummy;
	}

	public int timeShortPut(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j += 2) {
				dummy += memory.putShort(j, (short)j).hashCode();
			}
		}
		return dummy;
	}

	public int timeBytePut(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j++) {
				dummy += memory.putByte(j, (byte)j).hashCode();
			}
		}
		return dummy;
	}

	public int timeLongGet(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j += 8) {
				dummy += memory.getLong(j);
			}
		}
		return dummy;
	}

	public int timeIntGet(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j += 4) {
				dummy += memory.getInt(j);
			}
		}
		return dummy;
	}

	public int timeShortGet(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j += 2) {
				dummy += memory.getShort(j);
			}
		}
		return dummy;
	}

	public int timeByteGet(int times) {
		int dummy = 0;
		for (int i=0; i < times; i++) {
			for (int j=0; j < size; j++) {
				dummy += memory.getByte(j);
			}
		}
		return dummy;
	}

	private enum MemoryAccessFactory {
		BYTE_ARRAY {
			@Override @NotNull
			public MemoryAccessW create(@NotNull BoundsChecker checker, int size) {
				return MemoryAccessUnsafeImpl.wrap(checker, new byte[size]);
			}
		},

		BYTE_BUFFER {
			@Override @NotNull
			public MemoryAccessW create(@NotNull BoundsChecker checker, int size) {
				return MemoryAccessUnsafeImpl.wrap(checker, ByteBuffer.allocateDirect(size));
			}
		};
		
		@NotNull
		public MemoryAccessW create(@NotNull BoundsChecker checker, int size) {
			throw new UnsupportedOperationException();
		}
	}
}