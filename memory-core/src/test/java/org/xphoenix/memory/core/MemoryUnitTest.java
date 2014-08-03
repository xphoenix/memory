package org.xphoenix.memory.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xphoenix.memory.core.MemoryUnit;

public class MemoryUnitTest {
	
	@Test
	public void byteTest() {
		MemoryUnit target = MemoryUnit.BYTE;
		assertEquals("B",  1L, target.from(1, MemoryUnit.BYTE));
		assertEquals("KB", 1024L, target.from(1, MemoryUnit.KILOBYTE));
		assertEquals("MB", 1024*1024L, target.from(1, MemoryUnit.MEGABYTE));
		assertEquals("GB", 1024*1024*1024L, target.from(1, MemoryUnit.GIGABYTE));
		assertEquals("TB", 1024*1024*1024*1024L, target.from(1, MemoryUnit.TERABYTE));
	}
	
	@Test
	public void testGigabyte() {
		assertEquals("", Integer.MAX_VALUE, MemoryUnit.BYTE.from(2, MemoryUnit.GIGABYTE) - 1);
	}
}
