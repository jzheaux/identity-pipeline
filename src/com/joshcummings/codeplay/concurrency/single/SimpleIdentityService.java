package com.joshcummings.codeplay.concurrency.single;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;

import com.joshcummings.codeplay.concurrency.Identity;
import com.joshcummings.codeplay.concurrency.IdentityService;
import com.joshcummings.codeplay.concurrency.Person;

public class SimpleIdentityService implements IdentityService {
	private List<Identity> verifiedIdentities = new ArrayList<>();
	
	private static class MergeCandidate implements Comparable<MergeCandidate> {
		private final Identity candidate;
		private final Integer score;
		
		public MergeCandidate(Identity candidate, Integer score) {
			this.candidate = candidate;
			this.score = score;
		}
		public Identity getCandidate() {
			return candidate;
		}
		@Override
		public int compareTo(MergeCandidate that) {
			return this.score.compareTo(that.score);
		}
	}
	
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
			if ( id.getLock().tryLock() ) {
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
					// rollback, out of scope
				} finally {
					id.getLock().unlock();
				}
			}
		}
		
		verifiedIdentities.add(identity);
		return false;
	}

	@Override
	public Identity getOne(Predicate<Identity> pred) {
		for ( Identity i : verifiedIdentities ) {
			if ( pred.test(i) ) {
				return i;
			}
		}
		return null;
	}
	
	@Override
	public List<Identity> search(Predicate<Identity> pred) {
		List<Identity> filtered = new ArrayList<>();
		for ( Identity i : verifiedIdentities ) {
			if ( pred.test(i) ) {
				filtered.add(i);
			}
		}
		return filtered;
	}

}
