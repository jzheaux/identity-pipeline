package com.joshcummings.codeplay.concurrency.dependency;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import com.joshcummings.codeplay.concurrency.AddressVerifier;
import com.joshcummings.codeplay.concurrency.EmailFormatter;
import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.IdentityIterable;
import com.joshcummings.codeplay.concurrency.IdentityReader;
import com.joshcummings.codeplay.concurrency.IdentityService;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;
import com.joshcummings.codeplay.concurrency.NoValidAddressesException;
import com.joshcummings.codeplay.concurrency.PhoneNumberFormatter;
import com.joshcummings.codeplay.concurrency.StatsLedger;
import com.joshcummings.codeplay.concurrency.StatsLedger.StatsEntry;

/**
 * An identity pipeline that coordinates asynchronous dependencies using CountDownLatch.
 * 
 */
public class CountDownLatchIdentityPipeline {
	private MalformedIdentityRepository malformed; // fire and forget
	private IdentityReader identityReader;
	private AddressVerifier addressVerifier;
	private PhoneNumberFormatter phoneNumberFormatter;
	private EmailFormatter emailFormatter;
	private IdentityService identityService;
	private StatsLedger statsLedger;

	public CountDownLatchIdentityPipeline(MalformedIdentityRepository malformed, IdentityReader identityReader,
			AddressVerifier addressVerifier, PhoneNumberFormatter phoneNumberFormatter, EmailFormatter emailFormatter,
			IdentityService identityService, StatsLedger statsLedger) {
		this.malformed = malformed;
		this.identityReader = identityReader;
		this.addressVerifier = addressVerifier;
		this.phoneNumberFormatter = phoneNumberFormatter;
		this.emailFormatter = emailFormatter;
		this.identityService = identityService;
		this.statsLedger = statsLedger;
	}

	// putting operations of different natures into different thread pools allows them to grow differently
	// and can reduce contention
	private ExecutorService verifyPool = Executors.newWorkStealingPool();
	private ExecutorService persistPool = Executors.newWorkStealingPool();
	
	public void process(InputStream input) {
		StreamSupport.stream(new IdentityIterable(input, identityReader).spliterator(), true).forEach((identity) -> {
			System.out.println("Processing identity #" + identity.getId());
			try {
				CountDownLatch cdl = new CountDownLatch(3);

				verifyPool.submit(() -> {
					validateAddresses(identity);
					cdl.countDown();
				});
				verifyPool.submit(() -> {
					phoneNumberFormatter.format(identity);
					cdl.countDown();
				});
				verifyPool.submit(() -> {
					emailFormatter.format(identity);
					cdl.countDown();
				});

				persistPool.submit(() -> {
					try {
						// this line will block and wait for the above three calls to finish
						if ( cdl.await(3000, TimeUnit.MILLISECONDS) ) {
							if (!identityService.persistOrUpdateBestMatch(identity)) {
								statsLedger.recordEntry(new StatsEntry(identity));
							}
						} else {
							// we can read an error message here that is supplied by the verification, 
							// but it's been left out for simplicity
							malformed.addIdentity(identity, "Something went wrong with validation");
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
			} catch (Exception e) {
				malformed.addIdentity(identity, e.getMessage());
			}
		});
	}

	protected void validateAddresses(Identity identity) {
		addressVerifier.verify(identity.getAddresses());

		if (identity.getAddresses().stream().allMatch(a -> !a.isVerified())) {
			throw new NoValidAddressesException();
		}
	}
}
