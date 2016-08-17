package com.joshcummings.codeplay.concurrency;

import java.util.List;

public interface AddressVerifier {
	void verify(List<Address> address);
	
	default void close() {}
	
	default void throttleUp(int by) {
		throw new UnsupportedOperationException("This verifier cannot be throttled up!");
	}
	default void throttleDown(int by) {
		throw new UnsupportedOperationException("This verifier cannot be throttled down!");
	}

}
