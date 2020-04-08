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
	
	public static final Long DEFAULT_NB_ACCOUNTS_SEED = 420L;
	public static final Long DEFAULT_DATA_SEED = 42020L;
	public static final Long DEFAULT_ACCOUNT_SEED = 2020L;	
	
	protected RandomDataGenerator rnd;
	
	public class Entry {		

		long accountKey;
		long dataKey;
		int month;
		IdentityIndex idx;
		
		public IdentityIndex getIdentity() {
			if (idx==null) {
				idx = IdentityIndex.createFromLong(accountKey);
			}
			return idx;
		}
		
		public long getAccountKeyLong() {
			return accountKey;
		}

		public long getDataKey() {
			return dataKey;
		}

		public int getMonth() {
			return month;
		}
		
		public String getAccountID() {			
			return  RandomDataGenerator.genAccountNumber(getIdentity().getFirstNameIdx(), getIdentity().getLastNameIdx(), getIdentity().getStreetIdx(), getIdentity().getCityIdx(), getIdentity().getAccountIdx());
		}

		public String[] getMetaData() {			
			return getDataGenerator().generate(accountKey, dataKey, month);
		}

		public String toString() {
			return LongCodec.decode(accountKey).toString() + 
			String.format(" --- %d - %d", month, dataKey);
		}
	}

	public synchronized RandomDataGenerator getDataGenerator() {
		if (rnd==null) {
			rnd = new RandomDataGenerator(false, true);
			try {
				rnd.init();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return rnd;
	}
	
	public SequenceGenerator(long accountSeed, int nbMonths) {
		this(new Random(accountSeed), new Random(DEFAULT_DATA_SEED), new Random(DEFAULT_NB_ACCOUNTS_SEED), nbMonths);
	}

	public SequenceGenerator(int nbMonths) {
		this(new Random(DEFAULT_ACCOUNT_SEED), new Random(DEFAULT_DATA_SEED), new Random(DEFAULT_NB_ACCOUNTS_SEED), nbMonths);
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
