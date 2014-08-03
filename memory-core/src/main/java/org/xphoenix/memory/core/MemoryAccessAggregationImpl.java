package org.xphoenix.memory.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;

/**
 * Aggregates a few memory chunk into one continues area
 */
public class MemoryAccessAggregationImpl<T extends MemoryAccessW> implements MemoryAccessW {

	/*
	 * Total size of aggregation, i.e number of addressable bytes
	 */
	private final long size;

	/*
	 * Size of each segment in bytes
	 */
	private final long segmentSize;

	/*
	 * Offset in first segment
	 */
	private final long firstSegmentOffset;

	/*
	 * Last addressable index in segments
	 */
	private final long lastSegmentLimit;

	/*
	 * Array contains MemoryAccesss which are covers accessed memory region +
	 * one more null ref.
	 *
	 * SegmentOverflowControl expects to have cur and next MemoryAcess impls to
	 * write data. Last null element guarantee that next pointer exists for the
	 * last addressable segment (with the null value)
	 */
	private final @NotNull T[] segments;

	public MemoryAccessAggregationImpl (@NotNull T[] segments) {
		this (segments, 0);
	}

	public MemoryAccessAggregationImpl (@NotNull T[] segments, long firstSegmentOffset) {
		this(segments, firstSegmentOffset, segments[segments.length-2].size());
	}

	public MemoryAccessAggregationImpl (@NotNull T[] segments, long firstSegmentOffset, long lastSegmentLimit) {
		this.segments = segments;
		this.segmentSize = segments[0].size();
		this.firstSegmentOffset = firstSegmentOffset;

		// Check that array layout is correct
		// ------------------------------------------------------------------------
		// TODO: Strange requirement for external client. Historically that is done
		// to not copy arrays here. Maybe it is not necessary right now? Also we might
		// consider to change SegmentOverflowConstrol API
		if (segments[segments.length-1] != null) {
			throw new RuntimeException("Last segment must be null");
		}

		// Check segments size and calculate current access size
		long size = 0;
		for (int i =0; i < segments.length-1; i++) {
			MemoryAccessW acs  = segments[i];
			if (acs.size() != segmentSize) {
				throw new RuntimeException("All segments must be equal in size");
			} else if (acs.getBoundsChecker() != segments[0].getBoundsChecker()) {
				throw new RuntimeException("All segments must have the same bounds checker");
			} else if (acs.getByteOrderConvertor() != segments[0].getByteOrderConvertor()) {
				throw new RuntimeException("All segments must have the same byte order");
			}
			size += acs.size();
		}

		size -= firstSegmentOffset;
		size -= (segmentSize - lastSegmentLimit);

		this.size = size;
		this.lastSegmentLimit = lastSegmentLimit;
	}

	/**
	 * Returns all memory chunks in current aggregation
	 *
	 * @return memory chunks
	 */
	public T[] getSegments() {
		return segments;
	}

	/**
	 * Returns index of the first addressable byte in the very first segment in
	 * aggregation
	 *
	 * @return index of the first addressable byte
	 */
	public long getFirstSegmentOffset() {
		return firstSegmentOffset;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#size()
	 */
	@Override
	public long size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#toByteBuffer()
	 */
	@Override
	public ByteBuffer[] toByteBuffer() {
		ArrayList<ByteBuffer> result = new ArrayList<>(segments.length-1);
		for (int i=0; i < segments.length-1; i++) {
			result.addAll(Arrays.asList(segments[i].toByteBuffer()));
		}

		// Lets say first segment returns 2 buffers 1024 bytes each
		// and firstSegmentOffset is 1025 bytes.
		//
		// That means first buffer should be removed from result and
		// the second buffer position must be set to 1
		long offs = firstSegmentOffset;
		for (Iterator<ByteBuffer> it = result.iterator(); offs > 0 && it.hasNext(); ) {
			ByteBuffer buf = it.next();
			if (buf.limit() < offs) {
				offs -= buf.limit();
				it.remove();
			} else {
				buf.position((int)offs);
				offs = 0;
			}
		}

		// Now we need to remove buffer from the tail to
		// take into account last segment limit
		long limit = segments[segments.length-2].size() - lastSegmentLimit;
		for (ListIterator<ByteBuffer> it = result.listIterator(result.size()); limit > 0 && it.hasPrevious(); ) {
			ByteBuffer buf = it.previous();
			if (buf.remaining() < limit ) {
				it.remove();
				limit -= buf.remaining();
			} else {
				buf.limit(buf.limit() - (int)limit);
				limit = 0;
			}
		}
		return result.toArray(new ByteBuffer[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putBuffer(long, java.nio.ByteBuffer)
	 */
	@Override
	public MemoryAccessW putBuffer(long index, ByteBuffer value) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		segments[segment].putBuffer(offset, value);
		for (int i=segment+1; i < segments.length && value.hasRemaining(); i++) {
			segments[i].putBuffer(0, value);
		}

		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putBuffer(long, byte[], int, int)
	 */
	@Override
	public MemoryAccessW putBuffer(long index, byte[] value, int offs, int size) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);
		int rem = size;

		for (int i=segment; i < segments.length && rem > 0; i++) {
			int cpy = (int)Math.min(rem, segments[i].size() - offset);
			segments[i].putBuffer(offset, value, offs, cpy);

			rem  -= cpy;
			offs += cpy;
			offset = 0;
		}

		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putByte(long, byte)
	 */
	@Override
	public MemoryAccessW putByte(long index, byte value) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		ctl.putByte(offset, value, segments[segment],segments[segment+1]);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putChar(long, char)
	 */
	@Override
	public MemoryAccessW putChar(long index, char value) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		ctl.putChar(offset, value, segments[segment],segments[segment+1]);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putDouble(long, double)
	 */
	@Override
	public MemoryAccessW putDouble(long index, double value) {
		putLong(index, Double.doubleToLongBits(value));
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putFloat(long, float)
	 */
	@Override
	public MemoryAccessW putFloat(long index, float value) {
		putInt(index, Float.floatToIntBits(value));
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putInt(long, int)
	 */
	@Override
	public MemoryAccessW putInt(long index, int value) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		ctl.putInt(offset, value, segments[segment],segments[segment+1]);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putLong(long, long)
	 */
	@Override
	public MemoryAccessW putLong(long index, long value) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		ctl.putLong(offset, value, segments[segment],segments[segment+1]);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#putShort(long, short)
	 */
	@Override
	public MemoryAccessW putShort(long index, short value) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		ctl.putShort(offset, value, segments[segment],segments[segment+1]);
		return this;
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#compareAndSwap(long, int, int)
	 */
	@Override
	public boolean compareAndSwap(long index, int expected, int value) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccess#compareAndSwap(long, long, long)
	 */
	@Override
	public boolean compareAndSwap(long index, long expected, long value) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getBoundsChecker()
	 */
	@Override
	public BoundsChecker getBoundsChecker() {
		return segments[0].getBoundsChecker();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getByteOrderConvertor()
	 */
	@Override
	public ByteOrderConvertor getByteOrderConvertor() {
		return segments[0].getByteOrderConvertor();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getByte(long)
	 */
	@Override
	public byte getByte(long index) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		return ctl.getByte(offset, segments[segment], segments[segment+1]);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getBuffer(byte[], int, int)
	 */
	@Override
	public void getBuffer(byte[] buffer, int index, int size) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getChar(long)
	 */
	@Override
	public char getChar(long index) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		return ctl.getChar(offset, segments[segment], segments[segment+1]);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getDouble(long)
	 */
	@Override
	public double getDouble(long index) {
		return Double.longBitsToDouble(getLong(index));
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getFloat(long)
	 */
	@Override
	public float getFloat(long index) {
		return Float.intBitsToFloat(getInt(index));
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getInt(long)
	 */
	@Override
	public int getInt(long index) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		return ctl.getInt(offset, segments[segment], segments[segment+1]);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getLong(long)
	 */
	@Override
	public long getLong(long index) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		return ctl.getLong(offset, segments[segment], segments[segment+1]);
	}

	/* (non-Javadoc)
	 * @see org.xphoenix.memory.core.MemoryAccessR#getShort(long)
	 */
	@Override
	public short getShort(long index) {
		int segment = segment(index);
		int offset = segmentOffset(index, segment);

		SegmentOverflowControl ctl = overflow(segment, offset);
		return ctl.getShort(offset, segments[segment], segments[segment+1]);
	}

	/**
	 * Returns SegmentOverflowControl instance to be used for read/write operation
	 * by the given offset.
	 * 
	 * <p> SegmentOverflowControl takes care of corner cases when accessing indexes
	 * are locate in adjacent segments.
	 * </p>
	 * 
	 * @param segment segment contains desired index
	 * @param offset insegment offset of desired index
	 * @return OverflowControl instance to be used for memory access
	 */
	private SegmentOverflowControl overflow(int segment, int offset) {
		int sizeLeftInSegment = (int) (segments[segment].size() - offset);
		switch (sizeLeftInSegment) {
			case 0: {
				return SegmentOverflowControl.SIZE_0;
			}
			case 1: {
				return SegmentOverflowControl.SIZE_1;
			}
			case 2: {
				return SegmentOverflowControl.SIZE_2;
			}
			case 3: {
				return SegmentOverflowControl.SIZE_3;
			}
			case 4: {
				return SegmentOverflowControl.SIZE_4;
			}
			case 5: {
				return SegmentOverflowControl.SIZE_5;
			}
			case 6: {
				return SegmentOverflowControl.SIZE_6;
			}
			case 7: {
				return SegmentOverflowControl.SIZE_7;
			}

			default:
				return SegmentOverflowControl.SIZE_8_AND_MORE;
		}
	}

	/**
	 * Return segment for index
	 *
	 * @param index index
	 * @return segment for that index
	 */
	private int segment(long index) {
		return (int)((firstSegmentOffset + index) / this.segmentSize);
	}

	private int segmentOffset(long index, int segment) {
		return (int)((firstSegmentOffset + index) - segment * this.segmentSize);
	}

	/*
	 * Solves cross-segment write problem.
	 *
	 * Imagine our mapping has two underlying segments, 1K each:
	 *
	 * 0          1024       2048
	 * +-----------+-----------+
	 * |           |           |
	 * +-----------+-----------+
	 *
	 * During the call:
	 * mapping.putLong(1020, 10L);
	 *
	 * It is need to write first half of long to one segment and
	 * second half in other.
	 *
	 *
	 * To avoid a lot of compare operation on each write call, enum is use.
	 * Each enum instance knows how to write each type in segments array, i.e
	 * when 1 byte left in segment, when 2 bytes left, e.t.c
	 *
	 * Please note that we need no more then 2 segments to write anything except
	 * buffer, however as buffer is completely separate, more general, case it
	 * is handled in other way (see MemoryAccess).
	 * 
	 * Each enum constant has name SIZE_N, where N is the number of bytes left in the left 
	 * buffer, so SIZE_1 means that left buffer has 1 byte left
	 *
	 * @author andrphi
	 */
	// TODO: Im not sure about result byte encoding in different cases. It is need to be double
	// checked by tests. Cross segment write should results with exactely same byte sequence as
	// normal write operation would. Im pretty sure there is a bug here. For now however we are
	// happy with what we have (sic) :|
	enum SegmentOverflowControl {
		SIZE_0 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putByte(long index, byte value, MemoryAccessW cur, MemoryAccessW next) {
				next.putByte(index, value);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getByte(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public byte getByte(long index, MemoryAccessW cur, MemoryAccessW next) {
				return next.getByte(index);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putChar(long index, char value, MemoryAccessW cur, MemoryAccessW next) {
				next.putChar(index, value);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getChar(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public char getChar(long index, MemoryAccessW cur, MemoryAccessW next) {
				return next.getChar(index);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putShort(long index, short value, MemoryAccessW cur, MemoryAccessW next) {
				next.putShort(index, value);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getShort(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public short getShort(long index, MemoryAccessW cur, MemoryAccessW next) {
				return next.getShort(index);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putInt(long index, int value, MemoryAccessW cur, MemoryAccessW next) {
				next.putInt(index, value);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getInt(long, int, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			public int getInt(long index, int value, MemoryAccessW cur, MemoryAccessW next) {
				return next.getInt(index);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				next.putLong(index, value);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return next.getLong(index);
			}
		},

		SIZE_1 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putChar(long index, char value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putByte(index, (byte)((value & 0xFF00) >> 8));
				next.putByte(0, (byte)(value & 0x00FF));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getChar(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public char getChar(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (char)(
						cur.getByte(index) << 8 |
						next.getByte(0) & 0x00FF
						);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putShort(long index, short value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putByte(index, (byte)((value & 0xFF00) >> 8));
				next.putByte(0, (byte)(value & 0x00FF));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getShort(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public short getShort(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (short)(
						cur.getByte(index) << 8 |
						next.getByte(0) & 0x00FF
						);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putInt(long index, int value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putByte(index, (byte)((value & 0xFF000000) >> 24));
				next.putByte(0,    (byte)((value & 0x00FF0000) >> 16));
				next.putShort(1,  (short)((value & 0x0000FFFF) >> 00));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getInt(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public int getInt(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (int)(
						(cur.getByte(index) << 24) & 0xFF000000 |
						(next.getByte(0)    << 16) & 0x00FF0000 |
						(next.getShort(1)   << 00) & 0x0000FFFF
						);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putByte(index, (byte)((value & 0xFF00000000000000L) >> 56));
				next.putInt  (0,    (int)((value & 0x00FFFFFFFF000000L) >> 24));
				next.putShort(4,  (short)((value & 0x0000000000FFFF00L) >>  8));
				next.putByte (6,   (byte)((value & 0x00000000000000FFL)));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getByte(index) << 56) & 0xFF00000000000000L |
						((long)next.getInt(0)     << 24) & 0x00FFFFFFFF000000L |
						((long)next.getShort(4)   <<  8) & 0x0000000000FFFF00L |
						((long)next.getByte (6)   <<  0) & 0x00000000000000FFL
						);
			}
		},

		SIZE_2 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putInt(long index, int value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putShort(index, (short)((value & 0xFFFF0000) >> 16));
				next.putShort(0,    (short)((value & 0x0000FFFF) >> 00));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getInt(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public int getInt(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (int)(
						(cur.getShort(index) << 16) & 0xFFFF0000 |
						(next.getShort(0)    << 00) & 0x0000FFFF
						);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putShort (index,(short)((value & 0xFFFF000000000000L) >> 48));
				next.putInt  (0,      (int)((value & 0x0000FFFFFFFF0000L) >> 16));
				next.putShort(4,    (short)((value & 0x000000000000FFFFL) >> 00));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getShort(index) << 48) & 0xFFFF000000000000L |
						((long)next.getInt(0)      << 16) & 0x0000FFFFFFFF0000L |
						((long)next.getShort(4)    << 00) & 0x000000000000FFFFL
						);
			}
		},

		SIZE_3 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putInt(long index, int value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putShort(index, (short)((value & 0xFFFF0000) >>  16));
				cur.putByte(index+2, (byte)((value & 0x0000FF00) >>   8));
				next.putByte(0,      (byte)((value & 0x000000FF) >>   0));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getInt(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public int getInt(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (int)(
						(cur.getShort(index)  << 16) & 0xFFFF0000 |
						(cur.getByte(index+2) <<  8) & 0x0000FF00 |
						(next.getByte(0)           ) & 0x000000FF
						);
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putShort(index, (short)((value & 0xFFFF000000000000L) >> 48));
				cur.putByte(index+2, (byte)((value & 0x0000FF0000000000L) >> 40));
				next.putInt (0,       (int)((value & 0x000000FFFFFFFF00L) >>  8));
				next.putByte(4,      (byte)((value & 0x00000000000000FFL)));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getShort(index)  << 48) & 0xFFFF000000000000L |
						((long)cur.getByte(index+2) << 40) & 0x0000FF0000000000L |
						((long)next.getInt(0)       <<  8) & 0x000000FFFFFFFF00L |
						((long)next.getByte(4)      << 00) & 0x00000000000000FFL
						);
			}
		},

		SIZE_4 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putInt(index, (int)((value & 0xFFFFFFFF00000000L) >> 32));
				next.putInt(0,   (int)((value  & 0x00000000FFFFFFFFL) >> 00));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getInt(index) << 32) & 0xFFFFFFFF00000000L |
						((long)next.getInt(0)    << 00) & 0x00000000FFFFFFFFL
						);
			}
		},

		SIZE_5 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putInt(index,     (int)((value & 0xFFFFFFFF00000000L)  >> 32));
				cur.putByte(index+4, (byte)((value & 0x00000000FF000000L) >> 24));
				next.putShort(0,    (short)((value & 0x0000000000FFFF00L) >>  8));
				next.putByte (2,     (byte)((value & 0x00000000000000FFL)));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getInt(index)    << 32) & 0xFFFFFFFF00000000L |
						((long)cur.getByte(index+4) << 24) & 0x00000000FF000000L |
						((long)next.getShort(0)     <<  8) & 0x0000000000FFFF00L |
						((long)next.getByte(2)      <<  0) & 0x00000000000000FFL
						);
			}
		},
		SIZE_6 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putInt(index,       (int)((value & 0xFFFFFFFF00000000L) >> 32));
				cur.putShort(index+4, (short)((value & 0x00000000FFFF0000L) >> 16));
				next.putShort(0,      (short)((value & 0x000000000000FFFFL) >>  0));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getInt(index)     << 32) & 0xFFFFFFFF00000000L |
						((long)cur.getShort(index+4) << 16) & 0x00000000FFFF0000L |
						((long)next.getShort(0)      <<  0) & 0x000000000000FFFFL
						);
			}
		},

		SIZE_7 {
			/* (non-Javadoc)
			 * @see vn.ozzy.storage.impl.SegmentMappingsFile.MappingSegmentWriter#putByte(long, byte, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
				cur.putInt(index,       (int)((value & 0xFFFFFFFF00000000L) >> 32));
				cur.putShort(index+4, (short)((value & 0x00000000FFFF0000L) >> 16));
				cur.putByte(index+6,   (byte)((value & 0x000000000000FF00L) >>  8));
				next.putByte(0,        (byte)((value & 0x00000000000000FFL)));
			}

			/* (non-Javadoc)
			 * @see vn.ozzy.storage.api.memory.MemoryAccessAggregationImpl.SegmentOverflowControl#getLong(long, vn.ozzy.storage.api.memory.MemoryAccess, vn.ozzy.storage.api.memory.MemoryAccess)
			 */
			@Override
			public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
				return (long)(
						((long)cur.getInt(index)     << 32) & 0xFFFFFFFF00000000L |
						((long)cur.getShort(index+4) << 16) & 0x00000000FFFF0000L |
						((long)cur.getByte(index+6)  <<  8) & 0x000000000000FF00L |
						((long)next.getByte(0)       <<  0) & 0x00000000000000FFL
						);
			}
		},

		SIZE_8_AND_MORE;

		public void putByte(long index, byte value, MemoryAccessW cur, MemoryAccessW next) {
			cur.putByte(index, value);
		}

		public byte getByte(long index, MemoryAccessW cur, MemoryAccessW next) {
			return cur.getByte(index);
		}

		public void putChar(long index, char value, MemoryAccessW cur, MemoryAccessW next) {
			cur.putChar(index, value);
		}

		public char getChar(long index, MemoryAccessW cur, MemoryAccessW next) {
			return cur.getChar(index);
		}

		public void putShort(long index, short value, MemoryAccessW cur, MemoryAccessW next) {
			cur.putShort(index, value);
		}

		public short getShort(long index, MemoryAccessW cur, MemoryAccessW next) {
			return cur.getShort(index);
		}

		public void putInt(long index, int value, MemoryAccessW cur, MemoryAccessW next) {
			cur.putInt(index, value);
		}

		public int getInt(long index, MemoryAccessW cur, MemoryAccessW next) {
			return cur.getInt(index);
		}

		public void putLong(long index, long value, MemoryAccessW cur, MemoryAccessW next) {
			cur.putLong(index, value);
		}

		public long getLong(long index, MemoryAccessW cur, MemoryAccessW next) {
			return cur.getLong(index);
		}
	}
}