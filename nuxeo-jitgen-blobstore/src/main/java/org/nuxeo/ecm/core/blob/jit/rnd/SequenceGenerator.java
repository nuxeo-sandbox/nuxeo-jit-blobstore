package org.nuxeo.ecm.core.blob.jit.rnd;

import java.util.Random;

public class SequenceGenerator {

	protected Long currentIdentitySeed;
	protected IdentityIndex currentIdentityIdx;
	protected Long currentDataSeed;
	protected int month;
	protected final int nbMonths;	
	protected int nbAccounts=2;
	protected int accountIdx=0;

	protected final Random acccountSeedGen;
	protected final Random dataSeedGen;
	protected final Random acountNumberSeedGen;
	
	public static final Long NB_ACCOUNTS_SEED = 420L;
	public static final Long DATA_SEED = 42020L;
	public static final Long ACCOUNT_SEED = 2020L;	
	
	public class Entry {		

		long accountKey;
		long dataKey;
		int month;
		
		public IdentityIndex getIdentity() {
			return IdentityIndex.createFromLong(accountKey);
		}
		
		public long getAccountKey() {
			return accountKey;
		}

		public long getDataKey() {
			return dataKey;
		}

		public int getMonth() {
			return month;
		}

		public String toString() {
			return LongCodec.decode(accountKey).toString() + 
			String.format(" --- %d - %d", month, dataKey);
		}
	}
	
	public SequenceGenerator(int nbMonths) {
		this(new Random(ACCOUNT_SEED), new Random(DATA_SEED), new Random(NB_ACCOUNTS_SEED), nbMonths);
	}
	
	public SequenceGenerator(Random acccountSeedGen, Random dataSeedGen, Random acountNumberSeedGen, int nbMonths) {
		this.nbMonths=nbMonths;
		this.acccountSeedGen=acccountSeedGen;
		this.dataSeedGen = dataSeedGen;
		this.acountNumberSeedGen = acountNumberSeedGen;
		this.month=0;		
	}
	
	public Entry next() {

		Entry result = new Entry();
		if (accountIdx > nbAccounts || accountIdx ==0) {
			currentIdentitySeed = acccountSeedGen.nextLong();
			currentIdentityIdx =IdentityIndex.createFromLong(currentIdentitySeed);
			nbAccounts = 1 + acountNumberSeedGen.nextInt(3);
			accountIdx=1;
			currentIdentityIdx.setAccountIdx(accountIdx);
			//month = 0;		
		}
		if (month >= nbMonths ) {
			month = 0;			
			accountIdx++;
			currentIdentityIdx.setAccountIdx(accountIdx);
		}
		
		if (accountIdx > nbAccounts) {
			return next();
		}
		result.accountKey=currentIdentityIdx.asLongKey();
		result.dataKey= dataSeedGen.nextLong();
		result.month=month;
				
		month++;			
		return result;
	}

	
}
