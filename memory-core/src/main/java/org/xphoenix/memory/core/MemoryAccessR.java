package org.xphoenix.memory.core;

/**
 * Interface to a memory chunk
 * <p> API is designed to replace ByteBuffer. There are
 * a few main difference:
 * <ol>
 * 	<li> long index - to operates with memory chunk more then 2GB </li>
 * 	<li> optional bounds checking - in high performance applications bounds checks could takes
 * 		 a lot of time and be unacceptable </li>
 * 	<li> optional order convertor - convert byte order only when it is necessary. JIT will remove
 * 		 NO_OP calls in runtime </li>
 * </ol>
 * </p>
 * @author andrphi
 */
public interface MemoryAccessR {

	public abstract long size();

	public abstract BoundsChecker getBoundsChecker();

	public abstract ByteOrderConvertor getByteOrderConvertor();

	public abstract byte getByte(long index);

	public abstract void getBuffer(byte[] buffer, int index, int size);

	public abstract char getChar(long index);

	public abstract double getDouble(long index);

	public abstract float getFloat(long index);

	public abstract int getInt(long index);

	public abstract long getLong(long index);

	public abstract short getShort(long index);

}