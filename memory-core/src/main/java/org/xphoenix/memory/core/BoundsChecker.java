package org.xphoenix.memory.core;

public enum BoundsChecker {
	
	NO_OP {
		@Override
		public void checkBounds (long index, long size) {
		}		
	},
	
	REAL {
		@Override
		public void checkBounds (long index, long size) {
			if (index < 0 || index >= size) {
				throw new ArrayIndexOutOfBoundsException();
			}
		}		
	};
	
	public void checkBounds (long index, long size) {
		throw new RuntimeException("Method must be implemented in childs");
	}
}
