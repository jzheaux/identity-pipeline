package com.joshcummings.codeplay.concurrency.dependency;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.joshcummings.codeplay.concurrency.AddressVerifier;
import com.joshcummings.codeplay.concurrency.EmailFormatter;
import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.IdentityReader;
import com.joshcummings.codeplay.concurrency.IdentityService;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;
import com.joshcummings.codeplay.concurrency.NoValidAddressesException;
import com.joshcummings.codeplay.concurrency.PhoneNumberFormatter;
import com.joshcummings.codeplay.concurrency.StatsLedger;
import com.joshcummings.codeplay.concurrency.StatsLedger.StatsEntry;

/**
 * An identity pipeline that coordinates asynchronous dependencies using lambdas and the continuation-passing style.
 * 
 */
public class ContinuationPassingIdentityPipeline {
	private MalformedIdentityRepository malformed; // fire and forget
	private IdentityReader identityReader; 
	private AddressVerifier addressVerifier;
	private PhoneNumberFormatter phoneNumberFormatter;
	private EmailFormatter emailFormatter;
	private IdentityService identityService;
	private StatsLedger statsLedger;
		
	public ContinuationPassingIdentityPipeline(MalformedIdentityRepository malformed, IdentityReader identityReader, AddressVerifier addressVerifier,
			PhoneNumberFormatter phoneNumberFormatter, EmailFormatter emailFormatter, IdentityService identityService, StatsLedger statsLedger) {
		this.malformed = malformed;
		this.identityReader = identityReader;
		this.addressVerifier = addressVerifier;
		this.phoneNumberFormatter = phoneNumberFormatter;
		this.emailFormatter = emailFormatter;
		this.identityService = identityService;
		this.statsLedger = statsLedger;
	}
	
	private ExecutorService verifyPool = Executors.newWorkStealingPool();
	private ExecutorService persistPool = Executors.newWorkStealingPool();
	
	public void process(InputStream input, Runnable processCompleted) {
		read(input, processCompleted, (i) -> {
			format(i, this::fail, (i2) -> {
				persist(i2, this::fail, (i3) -> {
					statsLedger.recordEntry(new StatsEntry(i3));
				});
			});
		});
	}
	
	private void read(InputStream input, Runnable end, Consumer<Identity> next) {
		ForkJoinPool.commonPool().submit(() -> {
			Identity identity = identityReader.read(input);
 		    if ( identity != null ) {
				read(input, end, next);
				System.out.println("Processing identity #" + identity.getId());
				next.accept(identity);
			} else {
				end.run();
			}
		});
	}
	
	private void format(Identity identity, BiConsumer<Identity, Throwable> failed, Consumer<Identity> identityFormatted) {
		verifyPool.submit(() -> {
			phoneNumberFormatter.format(identity);
			emailFormatter.format(identity);
			try {
				validateAddresses(identity);
				identityFormatted.accept(identity);
			} catch ( NoValidAddressesException e ) {
				failed.accept(identity, e);
			}
		});
	}
	
	private void persist(Identity identity, BiConsumer<Identity, Throwable> failed, Consumer<Identity> identityPersisted) {
		persistPool.submit(() -> {
			if ( identityService.persistOrUpdateBestMatch(identity) ) {
				identityPersisted.accept(identity);
			}
		});
	}
	
	private void fail(Identity identity, Throwable t) {
		malformed.addIdentity(identity, t.getMessage());
	}
	
	private void validateAddresses(Identity identity) {
		addressVerifier.verify(identity.getAddresses());
		
		if ( identity.getAddresses().stream().allMatch(a -> !a.isVerified())) {
			throw new NoValidAddressesException();
		}
	}
}
