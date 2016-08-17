package com.joshcummings.codeplay.concurrency.aggregation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.IdentityService;
import com.joshcummings.codeplay.concurrency.Person;

/**
 * A thread-safe identity service that using ReentrantLock on each element instead of on the entire method.
 * 
 *
 */
public class ThreadSafeIdentityService implements IdentityService {
	private volatile Queue<Identity> verifiedIdentities = new ConcurrentLinkedQueue<>();

	@Override
	public boolean persistOrUpdateBestMatch(Identity identity) {
		class BestHolder { MergeCandidate best; }
		BestHolder is = new BestHolder();
		
		for ( Identity i : verifiedIdentities ) {
			if ( i.getLock().tryLock() ) {
				try {
					scoreMatch(identity, i).ifPresent(mergeable -> {
						if ( is.best == null || mergeable.getScore() > is.best.getScore() ) {
							if ( is.best != null ) is.best.getCandidate().getLock().unlock();
							is.best = mergeable;
							i.getLock().lock(); // get a second lock for the same thread
						}
					});
				} catch ( Exception e ) {
					// don't really eat!
				} finally {
					i.getLock().unlock();
				}
			}
		}	
		
		if ( is.best != null ) {
			try {
				Person candidate = (Person)is.best.getCandidate();
				merge((Person)identity, candidate);
				return true;
			} finally {
				is.best.getCandidate().getLock().unlock();
			}
		}
		
		verifiedIdentities.add(identity);
		
		return false;
	}

	private ExecutorService pool = Executors.newWorkStealingPool();
	
	// Notice the fact that we are submitting this inside a thread. The reason for this is to force
	// Java to use our provided thread pool instead of ForkJoinPool.commonPool()
	@Override
	public Identity getOne(Predicate<Identity> p) {
		try {
			return pool.submit(
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
			return pool.submit(
						() -> verifiedIdentities.parallelStream().filter(pred).collect(Collectors.toList())
					).get();
		} catch ( ExecutionException | InterruptedException e ) {
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		}
	}
	
	public Queue<Identity> getVerifiedIdentities() {
		return verifiedIdentities;
	}
	
	private Optional<MergeCandidate> scoreMatch(Identity incoming, Identity existing) {
		int score = 0;
		if ( existing.getEmailAddress() != null &&
				existing.getEmailAddress().equals(incoming.getEmailAddress()) ) {
			score += 50;
		}
		if ( existing.getPhoneNumber() != null && 
				existing.getPhoneNumber().equals(incoming.getPhoneNumber()) ) {
			score += 15;
		}
		if ( existing.getName().equals(incoming.getName()) ) {
			score += 35;
		}
		if ( score >= 50 ) {
			return Optional.of(new MergeCandidate(existing, score));
		}
		return Optional.empty();
	}
	
	private void merge(Person incoming, Person existing) {
		if ( existing.getEmailAddress() == null ) {
			existing.setEmailAddress(incoming.getEmailAddress());
		}
		if ( existing.getPhoneNumber() == null ) {
			existing.setPhoneNumber(incoming.getPhoneNumber());
		}
		existing.addAddresses(incoming.getAddresses());
	}
}
