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

public class LongCodec {

	public static final int FNAME_BITS = 17;
	public static final int FNAME_MAX = 1 << FNAME_BITS;

	public static final int LNAME_BITS = 17;
	public static final int LNAME_MAX = 1 << LNAME_BITS;

	public static final int CITIES_BITS = 15;
	public static final int CITIES_MAX = 1 << CITIES_BITS;

	public static final int STREET_BITS = 11;
	public static final int STREET_MAX = 1 << STREET_BITS;

	public static final int ACCOUNT_BITS = 3;
	public static final int ACCOUNT_MAX = 1 << ACCOUNT_BITS;

	public static Long encode(int fNameIdx, int lNameIdx, int streetIdx, int cityIdx, int accountIdx) {
		Long result = Long.valueOf(accountIdx);
		result = result | (Long.valueOf(fNameIdx) << ACCOUNT_BITS);
		result = result | (Long.valueOf(lNameIdx) << (ACCOUNT_BITS + FNAME_BITS));
		result = result | (Long.valueOf(cityIdx) << (ACCOUNT_BITS + FNAME_BITS + LNAME_BITS));
		result = result | (Long.valueOf(streetIdx) << (ACCOUNT_BITS + FNAME_BITS + LNAME_BITS + CITIES_BITS));
		return result;
	}

	public static IdentityIndex decode(Long key) {
		IdentityIndex idx = new IdentityIndex();
		idx.accountIdx = (int) (key & (ACCOUNT_MAX - 1));
		key = key >>> ACCOUNT_BITS;
		idx.firstNameIdx = (int) (key & (FNAME_MAX - 1));
		key = key >>> FNAME_BITS;
		idx.lastNameIdx = (int) (key & (LNAME_MAX - 1));
		key = key >>> LNAME_BITS;
		idx.cityIdx = (int) (key & (CITIES_MAX - 1));
		key = key >>> CITIES_BITS;
		idx.streetIdx = (int) (key & (STREET_MAX - 1));
		return idx;
	}

}
