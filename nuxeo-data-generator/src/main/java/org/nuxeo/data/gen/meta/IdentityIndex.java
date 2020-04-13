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