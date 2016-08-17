package com.joshcummings.codeplay.concurrency;

import java.io.InputStream;

public interface MalformedIdentityRepository {
	public void addIdentity(Identity identity, String reason);
	
	public void addIdentity(InputStream message, String reason);
}
