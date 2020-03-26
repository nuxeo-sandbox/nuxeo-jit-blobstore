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

package org.nuxeo.ecm.core.blob.jit.rnd.key;

public class DummyKeyCodec implements KeyCodec {

	protected static final String SEP = "@";

	@Override
	public String encodeSeeds(Long seed1, Long seed2, Integer dm) {
		StringBuffer sb = new StringBuffer();

		sb.append(String.format("%019d", seed1));
		if (seed2 != null) {
			sb.append(SEP);
			sb.append(String.format("%019d", seed2));
		}
		sb.append(SEP);
		sb.append(String.format("%04d", dm));
		return sb.toString();
	}

	@Override
	public Long[] decodeSeeds(String key) {
		String[] parts = key.split(SEP);
		return new Long[] { Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]) };
	}

}
