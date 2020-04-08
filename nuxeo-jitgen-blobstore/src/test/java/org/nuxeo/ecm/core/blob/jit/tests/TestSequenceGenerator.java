package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.nuxeo.ecm.core.blob.jit.rnd.IdentityIndex;
import org.nuxeo.ecm.core.blob.jit.rnd.RandomDataGenerator;
import org.nuxeo.ecm.core.blob.jit.rnd.SequenceGenerator;

public class TestSequenceGenerator {

	@Test
	public void checkSequenceGeneration() {
		
		int nbIdentity = 10;
		// use the same default sequence to determine number of account per identity
		Random nbAccountGen = new Random(SequenceGenerator.NB_ACCOUNTS_SEED);
		int nbMonths = 48;
		
		List<Integer> accounts = new ArrayList<Integer>();
		int nbTotalAccounts = 0;
		
		for (int i = 0; i < nbIdentity; i++) {
			int n = 1 + nbAccountGen.nextInt(3);
			nbTotalAccounts+=n;
			accounts.add(n);
		}
		
		SequenceGenerator sg = new SequenceGenerator(nbMonths);
		int lastMonth=-1;
		int lastAccount=0;
		int lastIdentity=-1;
		Set<Integer> identities = new HashSet<Integer>();
		
		for (int i = 0; i < nbTotalAccounts*nbMonths; i++) {
			
			SequenceGenerator.Entry e =sg.next();
		
			//System.out.println(e.toString());
			
			// months should be a sequence
			if (lastMonth>=0) {
				assertEquals((lastMonth+1)%nbMonths, e.getMonth());
			}
			lastMonth = e.getMonth();
			
			// Account should change every X months
			if (lastAccount>0) {
				if (i % nbMonths==0) {
					if (lastIdentity>=0) {
						if (lastIdentity==e.getIdentity().getFirstNameIdx()) {
							assertNotEquals(lastAccount, e.getIdentity().getAccountIdx());	
						} else {
							assertEquals(1, e.getIdentity().getAccountIdx());
						}						
					} else {
						assertEquals(01, e.getIdentity().getAccountIdx());	
					}
					
				} else {
					assertEquals(lastAccount, e.getIdentity().getAccountIdx());
				}
			}
			lastAccount = e.getIdentity().getAccountIdx();
			lastIdentity = e.getIdentity().getFirstNameIdx();
			identities.add(e.getIdentity().getFirstNameIdx());
		}
		assertEquals(nbIdentity, identities.size());
	}
	
	
	@Test
	public void checkNoDup() {		
		
		// default seed
		SequenceGenerator sg = new SequenceGenerator(1);
		int nbEntries = 1000000;
		Set<Long> allKeys = new HashSet<Long>();
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg.next().getAccountKey());			
		}		
		// check no dups
		assertEquals(nbEntries, allKeys.size());
		
		// new seed
		SequenceGenerator sg2 = new SequenceGenerator(1L, 1);
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg2.next().getAccountKey());			
		}
		// check no dups
		assertEquals(nbEntries*2, allKeys.size());

		// new seed
		SequenceGenerator sg3 = new SequenceGenerator(2L, 1);
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg3.next().getAccountKey());			
		}
		// check no dups
		assertEquals(nbEntries*3, allKeys.size());
		
		// recreate initial sequence
		sg = new SequenceGenerator(1);
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg.next().getAccountKey());			
		}		
		// should be all dups!
		assertEquals(3*nbEntries, allKeys.size());
				
	}	

	
	@Test
	public void checkSeqWithMonths() {		
		
		SequenceGenerator sg = new SequenceGenerator(1);
		int nbKeys = 10000;
		int nbEntries = 48*nbKeys;
		Set<Long> allKeys = new HashSet<Long>();
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg.next().getAccountKey());			
		}		
		assertEquals(nbEntries, allKeys.size());
		
		SequenceGenerator sg2 = new SequenceGenerator(48);
		Set<Long> allKeys2 = new HashSet<Long>();
		for (int i = 0; i < nbEntries; i++) {
			Long k = sg2.next().getAccountKey();			
			allKeys2.add(k);			
			assertTrue(allKeys.contains(k));			
		}		
		assertEquals(nbKeys, allKeys2.size());
		
	}

	@Test
	public void checkNoDupAccounts() {		
		
		SequenceGenerator sg = new SequenceGenerator(1);
		int nbEntries = 100000;
		Set<Long> allKeys = new HashSet<Long>();
		Set<String> allAccounts = new HashSet<String>();
			
		for (int i = 0; i < nbEntries; i++) {			
			SequenceGenerator.Entry e = sg.next();
			IdentityIndex idx = e.getIdentity();
			allKeys.add(e.getAccountKey());
			String accountKey = RandomDataGenerator.genAccountNumber(idx.getFirstNameIdx(), idx.getLastNameIdx(), idx.getStreetIdx(), idx.getCityIdx(), idx.getAccountIdx());
			allAccounts.add(accountKey);
		}		
		assertEquals(nbEntries, allKeys.size());
		assertEquals(nbEntries, allAccounts.size());
	}
}
