package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
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
}
