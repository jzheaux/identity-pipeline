package com.joshcummings.codeplay.concurrency.throttle;

import java.util.List;
import java.util.concurrent.Future;

import com.joshcummings.codeplay.concurrency.Address;

public interface Batcher {
	Future<?> submit(List<Address> jobs);
}
