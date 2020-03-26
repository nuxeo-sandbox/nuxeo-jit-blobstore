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

public class AsciiKeyCodec implements KeyCodec {

	protected static final String SEP = "-";
	protected static final String INV = "@";
	protected static final String PAD = "_";

	protected static final String CHARS = "0123456789" + "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	static final int B = CHARS.length();

	protected static final int LSIZE = 12;
	protected static final int SIZE = 4;

	public static Number decode(String s) {
		if (s.length() == LSIZE) {
			return decodeLong(s);
		} else {
			return decodeInt(s);
		}
	}

	protected static long decodeLong(String s) {
		s = clean(s);
		boolean invert = false;
		if (s.startsWith(INV)) {
			s = s.substring(1);
			invert = true;
		}
		long num = 0;
		for (char ch : s.toCharArray()) {
			num *= B;
			num += CHARS.indexOf(ch);
		}
		if (invert) {
			num = -num - 1;
		}
		return num;
	}

	protected static int decodeInt(String s) {
		s = clean(s);
		boolean invert = false;
		if (s.startsWith(INV)) {
			s = s.substring(1);
			invert = true;
		}

		int num = 0;
		for (char ch : s.toCharArray()) {
			num *= B;
			num += CHARS.indexOf(ch);
		}
		if (invert) {
			num = -num - 1;
		}
		return num;
	}

	protected static String pad(String s, int size) {
		return PAD.repeat(size - s.length()) + s;
	}

	protected static String clean(String s) {
		int idx = s.lastIndexOf(PAD);
		if (idx >= 0) {
			return s.substring(idx + 1);
		}
		return s;
	}

	public static String encode(long num) {
		return encode(num, LSIZE);
	}

	public static String encode(int num) {
		return encode(num, SIZE);
	}

	protected static String encode(long num, int padSize) {
		String prefix = null;
		if (num < 0) {
			prefix = INV;
			num = -(num + 1);
		}
		StringBuilder sb = new StringBuilder();
		while (num != 0) {
			sb.append(CHARS.charAt((int) (num % B)));
			num /= B;
		}
		if (prefix != null) {
			sb.append(prefix);
		}
		String encoded = sb.reverse().toString();
		return pad(encoded, padSize);
	}

	public String encodeSeeds(Long s1, Long s2, Integer m) {
		StringBuilder sb = new StringBuilder();
		sb.append(encode(s1));
		sb.append(SEP);
		sb.append(encode(s2));
		sb.append(SEP);
		sb.append(encode(m));
		return sb.toString();
	}

	public Long[] decodeSeeds(String key) {
		String[] parts = key.split(SEP);
		return new Long[] { decodeLong(parts[0]), decodeLong(parts[1]), Long.valueOf(decodeInt(parts[2])) };
	}
}
