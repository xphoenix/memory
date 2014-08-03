package org.xphoenix.memory.core;

import org.jetbrains.annotations.NotNull;

/**
 * Memory units as it is
 *
 * @author andrphi
 */
// TODO: Add network units, such as kilobits
// TODO: Add method for human readable translations
public enum MemoryUnit {
	BYTE 	 ("b",  "byte"),
	KILOBYTE ("kb", "kilobyte"),
	MEGABYTE ("mb", "megabyte"),
	GIGABYTE ("gb", "gigabyte"),
	TERABYTE ("tb", "terabyte");

	private final @NotNull String shortName;
	private final @NotNull String longName;

	private MemoryUnit(@NotNull String shortName, @NotNull String longName) {
		this.shortName = shortName;
		this.longName = longName;
	}

	public long from(long value, @NotNull MemoryUnit base) {
		int diff = base.ordinal() - ordinal();
		switch (Integer.signum(diff)) {
			case -1: {
				return (long)value >> (10 * -diff);
			}

			case 1: {
				return (long)value << (diff*10);
			}

			case 0: {
				return value;
			}

			default:
				throw new RuntimeException("Signum unexpected value");
		}
	}

	public int convertToInt(long value, @NotNull MemoryUnit base) {
		int diff = base.ordinal() - ordinal();
		long result;
		switch (Integer.signum(diff)) {
			case -1: {
				result = (long)value >> (10 * -diff);
				break;
			}

			case 1: {
				result = (long)value << (diff*10);
				break;
			}

			case 0: {
				result = value;
				break;
			}

			default:
				throw new RuntimeException("Signum unexpected value");
		}

		if (result >= Integer.MAX_VALUE) {
			throw new RuntimeException("Value is too big");
		}

		return (int)result;
	}
}
