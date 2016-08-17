package com.joshcummings.codeplay.concurrency.dependency;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.joshcummings.codeplay.concurrency.Address;
import com.joshcummings.codeplay.concurrency.AddressVerifier;
import com.joshcummings.codeplay.concurrency.EmailFormatter;
import com.joshcummings.codeplay.concurrency.FileIdentityReader;
import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.IdentityReader;
import com.joshcummings.codeplay.concurrency.IdentityService;
import com.joshcummings.codeplay.concurrency.MalformedIdentityRepository;
import com.joshcummings.codeplay.concurrency.NewFileIdentityReader;
import com.joshcummings.codeplay.concurrency.NoPasswordIdentityReader;
import com.joshcummings.codeplay.concurrency.PhoneNumberFormatter;
import com.joshcummings.codeplay.concurrency.SimpleEmailFormatter;
import com.joshcummings.codeplay.concurrency.SimplePhoneNumberFormatter;
import com.joshcummings.codeplay.concurrency.StatsLedger;
import com.joshcummings.codeplay.concurrency.aggregation.ThreadSafeIdentityService;
import com.joshcummings.codeplay.concurrency.aggregation.ThreadSafeStatsLedger;
import com.joshcummings.codeplay.concurrency.splitting.MultiStrategyIdentityReader;
import com.joshcummings.codeplay.concurrency.splitting.SingleThreadedScatterGatherer;

public class IdentityPipelineTest {
	private InputStream identities;
	
	private MalformedIdentityRepository malformed = 
			new MalformedIdentityRepository() {
				@Override
				public void addIdentity(Identity identity, String reason) {}

				@Override
				public void addIdentity(InputStream message, String reason) {}
	};
	
	private IdentityReader identityReader =
			new MultiStrategyIdentityReader(
					Arrays.asList(new NewFileIdentityReader(),
							new FileIdentityReader(),
							new NoPasswordIdentityReader()),
					new SingleThreadedScatterGatherer(), malformed);
	
	private AddressVerifier addressVerifier = 
			new AddressVerifier() {
				int j;
				
				@Override
				public void verify(List<Address> address) {
					for ( int i = 0; i < 10000000; i++ ) {
						j+=i;
					}
					address.stream().forEach(a -> a.setVerified(true));
				}
	};
	
	private PhoneNumberFormatter pFormatter = new SimplePhoneNumberFormatter();
	private EmailFormatter eFormatter = new SimpleEmailFormatter();
	private IdentityService identityService;
	private StatsLedger statsLedger = new ThreadSafeStatsLedger();
	
	private CountDownLatch cdl;
	
	@Before
	public void openFile() throws IOException {
		cdl = new CountDownLatch(Files.readAllLines(Paths.get("test/identities.csv")).size());
		identities = new FileInputStream("test/identities.csv") {
			@Override
			public int read() throws IOException {
				int ch = super.read();
				return ch;
			}
		};
		identityService = new ThreadSafeIdentityService() {
			@Override
			public boolean persistOrUpdateBestMatch(Identity identity) {
				boolean toReturn = super.persistOrUpdateBestMatch(identity);
				cdl.countDown();
				return toReturn;
			}
		};
	}
	
	@After
	public void closeFile() throws IOException {
		identities.close();
	}
	
	@Test
	public void testSingle() throws Exception {
		SingleThreadedIdentityPipeline ip = new SingleThreadedIdentityPipeline(
				malformed,
				identityReader,
				addressVerifier,
				pFormatter,
				eFormatter,
				identityService,
				statsLedger);
		
		try {
			ip.process(identities);
		} finally {
			List<Identity> list = identityService.search(i -> true);
			System.out.println(list.size());
		}
	}
	
	@Test
	public void testCountDownLatch() throws Exception {
		CountDownLatchIdentityPipeline ip = new CountDownLatchIdentityPipeline(
				malformed,
				identityReader,
				addressVerifier,
				pFormatter,
				eFormatter,
				identityService,
				statsLedger);
		
		try {
			ip.process(identities);
		} finally {
			cdl.await();
			List<Identity> list = identityService.search(i -> true);
			System.out.println(list.size());
			Assert.assertTrue(list.size() >= 6);
		}
	}
	
	@Test
	public void testContinuationPassing() throws Exception {
		ContinuationPassingIdentityPipeline ip = new ContinuationPassingIdentityPipeline(
				malformed,
				identityReader,
				addressVerifier,
				pFormatter,
				eFormatter,
				identityService,
				statsLedger);
		
		ip.process(identities, () -> {
			
		});
		try {
			cdl.await();
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		List<Identity> list = identityService.search(i -> true);
		Assert.assertTrue(list.size() >= 6);
	}
	
	@Test
	public void testCompletableFuture() throws Exception {
		CompletableFutureIdentityPipeline ip = new CompletableFutureIdentityPipeline(
				malformed,
				identityReader,
				addressVerifier,
				pFormatter,
				eFormatter,
				identityService,
				statsLedger);
		
		try {
			ip.process(identities);
		} finally {
			cdl.await();
			List<Identity> list = identityService.search(i -> true);
			Assert.assertTrue(list.size() >= 6);
		}
	}
}
