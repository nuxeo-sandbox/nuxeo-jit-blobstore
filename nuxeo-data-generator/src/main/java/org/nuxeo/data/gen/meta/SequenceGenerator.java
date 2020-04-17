/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 */
package org.nuxeo.data.gen.meta;

import java.util.Random;

public class SequenceGenerator {

	protected Long currentIdentitySeed;
	protected IdentityIndex currentIdentityIdx;
	protected Long currentDataSeed;
	protected int month;
	protected final int nbMonths;
	protected int nbAccounts = 2;
	protected int accountIdx = 0;

	protected final Random acccountSeedGen;
	protected final Random dataSeedGen;
	protected final Random acountNumberSeedGen;

	public static final Long DEFAULT_NB_ACCOUNTS_SEED = 420L;
	public static final Long DEFAULT_DATA_SEED = 42020L;
	public static final Long DEFAULT_ACCOUNT_SEED = 2020L;

	protected static RandomDataGenerator rnd;

	protected int monthOffset = 0;
	protected int accountsRange = 3;

	public boolean sync = true;

	public int getAccountsRange() {
		return accountsRange;
	}

	public void setAccountsRange(int accountsRange) {
		this.accountsRange = accountsRange;
	}	
	
	public int getMonthOffset() {
		return monthOffset;
	}

	public void setMonthOffset(int monthOffset) {
		this.monthOffset = monthOffset;
	}

	public void skip(long nb) {
		while (nb > 0) {
			next();
			nb--;
		}
	}

	public class Entry {

		long accountKey;
		long dataKey;
		int month;
		IdentityIndex idx;

		public IdentityIndex getIdentity() {
			if (idx == null) {
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
			return RandomDataGenerator.genAccountNumber(getIdentity().getFirstNameIdx(), getIdentity().getLastNameIdx(),
					getIdentity().getStreetIdx(), getIdentity().getCityIdx(), getIdentity().getAccountIdx());
		}

		public String[] getMetaData() {
			return getDataGenerator().generate(accountKey, dataKey, month);
		}

		public String toString() {
			return LongCodec.decode(accountKey).toString() + String.format(" --- %d - %d", month, dataKey);
		}
	}

	public static synchronized RandomDataGenerator getDataGenerator() {
		if (rnd == null) {
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
		this(new Random(DEFAULT_ACCOUNT_SEED), new Random(DEFAULT_DATA_SEED), new Random(DEFAULT_NB_ACCOUNTS_SEED),
				nbMonths);
	}

	public SequenceGenerator(Random acccountSeedGen, Random dataSeedGen, Random acountNumberSeedGen, int nbMonths) {
		this.nbMonths = nbMonths;
		this.acccountSeedGen = acccountSeedGen;
		this.dataSeedGen = dataSeedGen;
		this.acountNumberSeedGen = acountNumberSeedGen;
		this.month = 0;
	}

	public Entry next() {

		if (sync) {
			synchronized (this) {
				return _next();
			}
		}
		return _next();
	}

	protected Entry _next() {

		Entry result = new Entry();
		if (accountIdx > nbAccounts || accountIdx == 0) {
			currentIdentitySeed = acccountSeedGen.nextLong();
			currentIdentityIdx = IdentityIndex.createFromLong(currentIdentitySeed);
			nbAccounts = 1;
			if (accountsRange > 0) {
				nbAccounts += acountNumberSeedGen.nextInt(accountsRange);
			}
			accountIdx = 1;
			currentIdentityIdx.setAccountIdx(accountIdx);
			// month = 0;
		}
		if (month >= nbMonths) {
			month = 0;
			accountIdx++;
			currentIdentityIdx.setAccountIdx(accountIdx);
		}

		if (accountIdx > nbAccounts) {
			return next();
		}
		result.accountKey = currentIdentityIdx.asLongKey();
		result.dataKey = dataSeedGen.nextLong();
		result.month = month + monthOffset;

		month++;
		return result;
	}
}
