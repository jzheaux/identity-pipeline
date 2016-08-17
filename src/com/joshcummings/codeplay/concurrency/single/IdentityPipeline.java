package com.joshcummings.codeplay.concurrency.single;

import java.io.InputStream;

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

public class IdentityPipeline {
	private MalformedIdentityRepository malformed;
	private IdentityReader identityReader; 
	private AddressVerifier addressVerifier;
	private PhoneNumberFormatter phoneNumberFormatter;
	private EmailFormatter emailFormatter;
	private IdentityService identityService;
	private StatsLedger statsLedger;
		
	public IdentityPipeline(MalformedIdentityRepository malformed, IdentityReader identityReader, AddressVerifier addressVerifier,
			PhoneNumberFormatter phoneNumberFormatter, EmailFormatter emailFormatter, IdentityService identityService, StatsLedger statsLedger) {
		this.malformed = malformed;
		this.identityReader = identityReader;
		this.addressVerifier = addressVerifier;
		this.phoneNumberFormatter = phoneNumberFormatter;
		this.emailFormatter = emailFormatter;
		this.identityService = identityService;
		this.statsLedger = statsLedger;
	}
	
	public void process(InputStream input) {
		Identity i;
		while ( ( i = readIdentity(input) ) != null ){
			final Identity identity = i;
			try {
				validateAddresses(identity);
				
				phoneNumberFormatter.format(identity);
				emailFormatter.format(identity);

				if ( !identityService.persistOrUpdateBestMatch(identity) ) {
					statsLedger.recordEntry(new StatsEntry(identity));
				}
			} catch ( Exception e ) {
				malformed.addIdentity(identity, e.getMessage());
			}
		}
	}
	
	private void validateAddresses(Identity identity) {
		addressVerifier.verify(identity.getAddresses());
		
		if ( identity.getAddresses().stream().allMatch(a -> !a.isVerified())) {
			throw new NoValidAddressesException();
		}
	}
	
	private Identity readIdentity(InputStream is) {
		return identityReader.read(is);
	}
}
