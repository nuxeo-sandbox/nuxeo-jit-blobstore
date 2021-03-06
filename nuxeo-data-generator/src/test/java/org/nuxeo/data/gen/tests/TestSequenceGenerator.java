package org.nuxeo.data.gen.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.nuxeo.data.gen.meta.IdentityIndex;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.meta.SequenceGenerator;

public class TestSequenceGenerator {

    protected static List<String> expectedAccountID=Arrays.asList(
    		"0E570-08A8E-53AE1E6-01",
    		"0D377-0F93A-3A16029-01",
    		"0D377-0F93A-3A16029-02",
    		"0D377-0F93A-3A16029-03",
    		"07180-05BE9-44A4265-01",
    		"07180-05BE9-44A4265-02",
    		"13A20-0E86C-69A4770-01",
    		"19E08-06256-59DD19E-01",
    		"05F4E-0100B-044C4D7-01",
    		"05F4E-0100B-044C4D7-02"  );

	@Test
	public void checkSequenceGeneration() {
		
		int nbIdentity = 10;
		// use the same default sequence to determine number of account per identity
		Random nbAccountGen = new Random(SequenceGenerator.DEFAULT_NB_ACCOUNTS_SEED);
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
			allKeys.add(sg.next().getAccountKeyLong());			
		}		
		// check no dups
		assertEquals(nbEntries, allKeys.size());
		
		// new seed
		SequenceGenerator sg2 = new SequenceGenerator(1L, 1);
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg2.next().getAccountKeyLong());			
		}
		// check no dups
		assertEquals(nbEntries*2, allKeys.size());

		// new seed
		SequenceGenerator sg3 = new SequenceGenerator(2L, 1);
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg3.next().getAccountKeyLong());			
		}
		// check no dups
		assertEquals(nbEntries*3, allKeys.size());
		
		// recreate initial sequence
		sg = new SequenceGenerator(1);
		for (int i = 0; i < nbEntries; i++) {
			allKeys.add(sg.next().getAccountKeyLong());			
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
			allKeys.add(sg.next().getAccountKeyLong());			
		}		
		assertEquals(nbEntries, allKeys.size());
		
		SequenceGenerator sg2 = new SequenceGenerator(48);
		Set<Long> allKeys2 = new HashSet<Long>();
		for (int i = 0; i < nbEntries; i++) {
			Long k = sg2.next().getAccountKeyLong();			
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
			allKeys.add(e.getAccountKeyLong());
			String accountKey = RandomDataGenerator.genAccountNumber(idx.getFirstNameIdx(), idx.getLastNameIdx(), idx.getStreetIdx(), idx.getCityIdx(), idx.getAccountIdx());
			allAccounts.add(accountKey);
		}		
		assertEquals(nbEntries, allKeys.size());
		assertEquals(nbEntries, allAccounts.size());
	}
	
    @Test
    public void canUseSequenceGenerator() throws Exception {

    	int nbAccountsToGenerate=10;
    	int nbMonths=48;

    	SequenceGenerator sGen = new SequenceGenerator(nbMonths);    	    	
    	
    	for (int i = 0; i < nbAccountsToGenerate*nbMonths; i++) {
    		 
    		SequenceGenerator.Entry entry = sGen.next();
    		
    		String accountId = entry.getAccountID();    		
    		assertTrue(expectedAccountID.contains(accountId));
    		
    		String[] meta = entry.getMetaData();
    		
    		System.out.println(accountId + "," + String.join(",", meta));
    	}

    }

    
	@Test
	public void checkConcurrency() throws Exception {		
		
		SequenceGenerator sg = new SequenceGenerator(1);		
		sg.sync=false;
		
		int nbThreads = 48;
		int nbKeys = nbThreads* 100000;
		
		long t0 = System.currentTimeMillis();
		Set<Long> allKeys = new HashSet<Long>();
		for (int i = 0; i < nbKeys; i++) {
			allKeys.add(sg.next().getAccountKeyLong());			
		}		
		long t1 = System.currentTimeMillis();
		Double throughput = nbKeys* 1.0 / (t1 - t0);
		System.out.println("Throughput mono-thread (K/ms):" + throughput);

		assertEquals(nbKeys, allKeys.size());				
		
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
		executor.prestartAllCoreThreads();
		Set<Long> allKeys2 = new HashSet<Long>();

		SequenceGenerator sg2 = new SequenceGenerator(1);				
		
		final class Task implements Runnable {

			Set<Long> keys = new HashSet<Long>();			
			@Override
			public void run() {
				for (int i =0; i < nbKeys/nbThreads; i++) {
					keys.add(sg2.next().getAccountKeyLong());
				}				
			}
			
			Set<Long> getKeys() {
				return keys;
			}		
		}

		t0 = System.currentTimeMillis();
		List<Task> tasks = new ArrayList<Task>();		
		for (int i =0; i < nbThreads; i++) {
			Task t = new Task();
			tasks.add(t);
			executor.submit(t);
		}
		
		executor.shutdown();
		boolean finished = executor.awaitTermination(3 * 60, TimeUnit.SECONDS);
		if (!finished) {
			System.out.println("Timeout !!!!");			
		}
		assertTrue(finished);

		t1 = System.currentTimeMillis();
		throughput = nbKeys* 1.0 / (t1 - t0);
		System.out.println("Throughput (K/ms):" + throughput);

		
		for (Task t : tasks) {
			allKeys2.addAll(t.getKeys());
		}

		assertEquals(nbKeys, allKeys2.size());
		for (Long k : allKeys2) {
			assertTrue(allKeys.contains(k));
		}
		
	}

	
	@Test
	public void checkContinuousSeq() {		
		
		int nbEntries = 5000;
		SequenceGenerator sg = new SequenceGenerator(12);

		Set<String> allAccounts = new HashSet<String>();
		Set<String> allDates = new HashSet<String>();
		
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			allAccounts.add(e.getAccountID());
			allDates.add(e.getMetaData()[4]);
			//System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
		}		

		// verify that we can regenerate the same sequence of accounts but with a different date
		sg = new SequenceGenerator(12);
		sg.setMonthOffset(12);		
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			assertTrue(allAccounts.contains(e.getAccountID()));
			assertFalse(allDates.contains(e.getMetaData()[4]));
			//System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
		}		
	}

	
	@Test
	public void testDateParse() throws Exception {
		

		SimpleDateFormat sdf1 = new SimpleDateFormat("MMM dd YYYY");		
		SimpleDateFormat sdf2 = new SimpleDateFormat("MMM dd yyyy");
		
		String strDate1 = sdf1.format(new Date());
		String strDate2 = sdf2.format(new Date());
		System.out.println(strDate1);
		System.out.println(strDate2);
		
		
		System.out.println(sdf1.parse(strDate1));
		System.out.println(sdf1.parse(strDate2));

		System.out.println(sdf2.parse(strDate1));
		System.out.println(sdf2.parse(strDate2));
		
	}
	
	@Test
	public void checkWorkAroundBug() throws Exception {		
		
		
		int nbEntries = 60;
		SequenceGenerator sg = new SequenceGenerator(60);	
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
			System.out.println(RandomDataGenerator.df.get().parse(e.getMetaData()[4].trim()));
		}	
		
		System.out.println("Live statements");
		// 6 last months of 2020
		nbEntries = 6;
		sg = new SequenceGenerator(60);	
		sg.setMonthOffset(6);
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
		}	

		System.out.println("Live statements");
		// 6 last months of 2020
		nbEntries = 6;
		sg = new SequenceGenerator(6);	
		sg.setMonthOffset(6);
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
		}	

		System.out.println("Achive -recent");
		// 6 previous months of 2020
		nbEntries = 6;
		sg = new SequenceGenerator(60);	
		sg.setMonthOffset(0);
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
		}	

		System.out.println("Achive -old");
		// 48 previous months of 2019=>2016
		nbEntries = 48;
		sg = new SequenceGenerator(60);	
		sg.setMonthOffset(12);
		for (int i = 0; i < nbEntries; i++) {
			SequenceGenerator.Entry e = sg.next();
			System.out.println(e.getAccountID() + " --- " + e.getMetaData()[4]);
		}	
		
		
	}

}
