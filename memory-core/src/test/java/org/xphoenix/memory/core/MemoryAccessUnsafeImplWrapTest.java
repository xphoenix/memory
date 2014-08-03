package org.xphoenix.memory.core;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;
import org.xphoenix.memory.core.BoundsChecker;
import org.xphoenix.memory.core.ByteOrderConvertor;
import org.xphoenix.memory.core.MemoryAccessW;
import org.xphoenix.memory.core.MemoryAccessUnsafeImpl;

public class MemoryAccessUnsafeImplWrapTest {
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonDirectWrap() {
		ByteBuffer nonDirect = ByteBuffer.allocate(10);
		MemoryAccessUnsafeImpl.wrap(nonDirect);
	}
	
	@Test
	public void testByteArrayWrapDefaults() {
		byte []data = new byte[0];
		
		MemoryAccessW acs = MemoryAccessUnsafeImpl.wrap(data);
		assertSame("BoundsChecker", BoundsChecker.REAL, acs.getBoundsChecker());
		assertSame("ByteConvertor", ByteOrderConvertor.NO_OP, acs.getByteOrderConvertor());

		acs = MemoryAccessUnsafeImpl.wrap(BoundsChecker.NO_OP, data);
		assertSame("BoundsChecker", BoundsChecker.NO_OP, acs.getBoundsChecker());
		assertSame("ByteConvertor", ByteOrderConvertor.NO_OP, acs.getByteOrderConvertor());
	}

	@Test
	public void testByteBufferWrapDefaults() {
		ByteBuffer data = ByteBuffer.allocateDirect(0);
		if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
			data.order(ByteOrder.BIG_ENDIAN);
		} else {
			data.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		MemoryAccessW acs = MemoryAccessUnsafeImpl.wrap(data);
		assertSame("BoundsChecker", BoundsChecker.REAL, acs.getBoundsChecker());
		assertSame("ByteConvertor", ByteOrderConvertor.SWAP, acs.getByteOrderConvertor());

		acs = MemoryAccessUnsafeImpl.wrap(BoundsChecker.NO_OP, data);
		assertSame("BoundsChecker", BoundsChecker.NO_OP, acs.getBoundsChecker());
		assertSame("ByteConvertor", ByteOrderConvertor.SWAP, acs.getByteOrderConvertor());
	}

	@Test
	public void testDirectWrap() {
		ByteBuffer direct = ByteBuffer.allocateDirect(10);
		direct.putInt(0, 10);
		
		MemoryAccessW access = MemoryAccessUnsafeImpl.wrap(direct);
		assertEquals("Size", 10, access.size());
		assertEquals("Value", 10, access.getInt(0));
		
		access.putInt(4, 20);
		assertEquals("Value#buf", 20, direct.getInt(4));
	}
}
