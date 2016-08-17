package com.joshcummings.codeplay.concurrency.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.IdentityService;
import com.joshcummings.codeplay.concurrency.Person;

public class StreamedIdentityService implements IdentityService {
	private volatile List<Identity> verifiedIdentities = new ArrayList<>();
	
	private ExecutorService es = Executors.newWorkStealingPool();
	
	@Override
	public boolean persistOrUpdateBestMatch(Identity identity) {
		PriorityQueue<MergeCandidate> candidates = new PriorityQueue<>();
		
		// find candidates
		for (Identity i : verifiedIdentities) {
			int score = 0;
			if ( i.getEmailAddress() != null && i.getEmailAddress().equals(identity.getEmailAddress()) ) {
				score += 50;
			}
			if ( i.getPhoneNumber() != null && i.getPhoneNumber().equals(identity.getPhoneNumber()) ) {
				score += 15;
			}
			if ( i.getName().equals(identity.getName()) ) {
				score += 35;
			}
			if ( score >= 50 ) {
				candidates.offer(new MergeCandidate(i, score));
			}
		}
		
		// pick the best one and lock on it
		for ( MergeCandidate candidate : candidates ) {
			Person id = (Person)candidate.getCandidate();
			if ( id.getLock().tryLock() ) { // we are locking this against other threads processing other identities
				try {
					if ( id.getEmailAddress() == null ) {
						id.setEmailAddress(identity.getEmailAddress());
					}
					if ( id.getPhoneNumber() == null ) {
						id.setPhoneNumber(identity.getPhoneNumber());
					}
					id.addAddresses(identity.getAddresses());
					return true;
				} catch ( Exception e ) {
					// rollback, out of scope for this demo
				} finally {
					id.getLock().unlock();
				}
			}
		}	
		
		synchronized ( verifiedIdentities ) {
			verifiedIdentities.add(identity);
		}
		
		return false;
	}

	// Notice the fact that we are submitting this inside a thread. The reason for this is to force
	// Java to use our provided thread pool instead of ForkJoinPool.commonPool()
	@Override
	public Identity getOne(Predicate<Identity> p) {
		try {
			return es.submit(
						() -> verifiedIdentities.parallelStream().filter(p).findAny().get()
					).get(); 
		} catch ( ExecutionException | InterruptedException e ) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
	
	@Override
	public List<Identity> search(Predicate<Identity> pred) {
		try {
			return es.submit(
						() -> verifiedIdentities.parallelStream().filter(pred).collect(Collectors.toList())
					).get();
		} catch ( ExecutionException | InterruptedException e ) {
			Thread.currentThread().interrupt();
			return new ArrayList<>();
		}
	}
}
