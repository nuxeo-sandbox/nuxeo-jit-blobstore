package org.nuxeo.data.gen.meta;

import java.util.Random;

public class IdentityIndex {

	protected int firstNameIdx;
	protected int lastNameIdx;
	protected int streetIdx;
	protected int cityIdx;
	protected int accountIdx;

	protected static Random rnd = new Random(2020L);
	
	public String toString() {
		return String.format("First: %06d - Last: %06d - Street: %04d - City: %05d - Account: %03d", firstNameIdx,
				lastNameIdx, streetIdx, cityIdx, accountIdx);
	}

	public static IdentityIndex createFromLong(long key) {	
		return LongCodec.decode(key);				
	}
	
	public static IdentityIndex createRandom() {
		return createRandom(rnd);
	}
	
	public static IdentityIndex createRandom(Random rndSequence) {	
		return LongCodec.decode(rndSequence.nextLong());				
	}	
	
	public int getFirstNameIdx() {
		return firstNameIdx;
	}

	public void setFirstNameIdx(int firstNameIdx) {
		this.firstNameIdx = firstNameIdx;
	}

	public int getLastNameIdx() {
		return lastNameIdx;
	}

	public void setLastNameIdx(int lastNameIdx) {
		this.lastNameIdx = lastNameIdx;
	}

	public int getStreetIdx() {
		return streetIdx;
	}

	public void setStreetIdx(int streetIdx) {
		this.streetIdx = streetIdx;
	}

	public int getCityIdx() {
		return cityIdx;
	}

	public void setCityIdx(int cityIdx) {
		this.cityIdx = cityIdx;
	}

	public int getAccountIdx() {
		return accountIdx;
	}

	public void setAccountIdx(int accountIdx) {
		this.accountIdx = accountIdx;
	}
	
	public Long asLongKey() {
		return LongCodec.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
	}
	
	
}