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

package org.nuxeo.ecm.core.blob.jit.rnd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;

import org.nuxeo.ecm.core.blob.jit.rnd.key.AsciiKeyCodec;
import org.nuxeo.ecm.core.blob.jit.rnd.key.DummyKeyCodec;
import org.nuxeo.ecm.core.blob.jit.rnd.key.KeyCodec;

public class RandomDataGenerator {

	public static final int START_YEAR = 2020;
	
	protected ArrayList<String> firstNames = new ArrayList<String>();
	protected ArrayList<String> lastNames = new ArrayList<String>();
	protected ArrayList<String> streets = new ArrayList<String>();
	protected ArrayList<String> cities = new ArrayList<String>();
	protected ArrayList<String> states = new ArrayList<String>();
	protected ArrayList<String> companies = new ArrayList<String>();

	// protected SimpleDateFormat df = new SimpleDateFormat("MMM dd YYYY");
	protected static final int DR = 5 * 365 * 24 * 3600 * 1000;

	protected final boolean generateOperations;

	protected final KeyCodec codec;	

	protected final boolean useLongDecodingForUserInfo=true;
	
	public static ThreadLocal<SimpleDateFormat> df = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("MMM dd YYYY");
		}
	};

	public RandomDataGenerator(boolean generateOperations, boolean asciiKey) {
		this.generateOperations = generateOperations;
		if (asciiKey) {
			codec = new AsciiKeyCodec();
		} else {
			codec = new DummyKeyCodec();
		}
	}

	protected String clean(String input) {
		return clean(input, 20);
	}
	
	protected String clean(String input, int len) {
		input = input.trim();
		if (input.startsWith("\"")) {
			input = input.substring(1);
		}		
		if (input.length() > len) {
			input = input.substring(0, len);
		}
		return input;
	}

	public void init() throws Exception {
		InputStream csv = this.getClass().getResourceAsStream("/data.csv");
		init(csv);
	}
	
	public void init(InputStream csv) throws Exception {
		init(new InputStreamReader(csv));
	}

	public void init(File csv) throws Exception {
		init(new FileReader(csv));
	}

	protected void init(Reader csvReader) throws Exception {

		try (BufferedReader reader = new BufferedReader(csvReader)) {

			String line = reader.readLine(); // skip first line
			line = reader.readLine();
			do {
				String parts[] = line.split(",");

				if (parts[0] != null)
					firstNames.add(clean(parts[0]));
				if (parts.length > 1 && parts[1] != null && !parts[1].isEmpty())
					lastNames.add(clean(parts[1]));
				if (parts.length > 2 && parts[2] != null && !parts[2].isEmpty())
					streets.add(clean(parts[2]));
				if (parts.length > 3 && parts[3] != null && !parts[3].isEmpty()) {
					cities.add(clean(parts[3]));
					if (parts.length > 5 && parts[5] != null && !parts[5].isEmpty()) {
						states.add(clean(parts[5]));
					} else {
						states.add("NY");
					}
				}
				if (parts.length > 6 && parts[6] != null && !parts[6].isEmpty())
					companies.add(clean(parts[6], 30));

				line = reader.readLine();
			} while (line != null);
		}
	}

	public String[] generate() {
		return generate(null, null, null);
	}

	public String seeds2Id(Long seed1, Long seed2, Integer dm) {
		return codec.encodeSeeds(seed1, seed2, dm);
	}

	protected Long[] Id2Seeds(String key) {
		return codec.decodeSeeds(key);
	}

	public String[] generate(String key) {
		Long[] seeds = Id2Seeds(key);
		return generate(seeds[0], seeds[1], seeds[2].intValue());
	}

	public String[] generate(Long seed1, Long seed2, Integer dm) {
		String[] result = null;

		if (seed1 == null) {
			seed1 = new Random().nextLong();
		}		

		if (dm == null) {
			dm = new Random().nextInt(48);
		}

		if (generateOperations) {
			result = new String[6 + 14 * 2 + 1 + 1 + 1 + 1];
		} else {
			result = new String[6 + 1];
		}
		
		if (useLongDecodingForUserInfo) {
			fillUserInfo(result, seed1);	
		} else {
			Random rndSeq1 = new Random(seed1);
			fillUserInfo(result, rndSeq1);			
		}
		
		Date date = getDateWithOffset(dm);
		try {
			result[4] = pad(df.get().format(date), 20, false);
		} catch (Exception e) {
			System.out.println("dm = " + dm);
			e.printStackTrace();
		}

		if (generateOperations) {
			if (seed2 == null) {
				seed2 = new Random().nextLong();
			}
			Random rndSeq2 = new Random(seed2);
			fillOperations(result, rndSeq2);
		}

		// store seed key
		result[result.length - 1] = seeds2Id(seed1, seed2, dm);

		return result;
	}


	public Date getDateWithOffset(int dm) {
		int dy = dm / 12;
		int m = dm - dy * 12;
		return new GregorianCalendar(START_YEAR - dy, m, 01).getTime();
	}


	public static String genAccountNumber(Random seqGen) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("%04d", Math.round(seqGen.nextDouble() * 9999L)));
		sb.append("-");
		sb.append(String.format("%04d", Math.round(seqGen.nextDouble() * 9999L)));
		sb.append("-");
		sb.append(String.format("%04d", Math.round(seqGen.nextDouble() * 9999L)));
		sb.append("-");
		sb.append(String.format("%05d", Math.round(seqGen.nextDouble() * 99999L)));
		
		return sb.toString();
	}

	public static String genAccountNumber(int fNameIdx, int lNameIdx, int streetIdx, int cityIdx, int accountIdx) {
		StringBuilder sb = new StringBuilder();		
		sb.append(String.format("%05X", fNameIdx));
		sb.append("-");
		sb.append(String.format("%05X", lNameIdx));
		sb.append("-");
		sb.append(String.format("%04X", cityIdx));
		sb.append(String.format("%03X", streetIdx));
		sb.append("-");
		sb.append(String.format("%02d", accountIdx));		
		return sb.toString();
	}

	
	protected void fillUserInfo(String[] result, Long seed1) {
		
		IdentityIndex idx = LongCodec.decode(seed1);
		fillUserInfo(result, idx.firstNameIdx, idx.lastNameIdx, idx.streetIdx, idx.cityIdx, idx.accountIdx);
	}
	
	protected void fillUserInfo(String[] result, int fNameIdx, int lNameIdx, int streetIdx, int cityIdx, int accountIdx) {
		
		//fNameIdx = fNameIdx % firstNames.size();
		//lNameIdx = lNameIdx % lastNames.size();
		//streetIdx = streetIdx % streets.size();
		//cityIdx = cityIdx % cities.size();

		fNameIdx = fNameIdx % LongCodec.FNAME_MAX;
		lNameIdx = lNameIdx % LongCodec.LNAME_MAX;
		streetIdx = streetIdx % LongCodec.STREET_MAX;
		cityIdx = cityIdx % LongCodec.CITIES_MAX;

		result[0] = firstNames.get(fNameIdx) + " " + lastNames.get(lNameIdx);
		result[1] = streets.get(streetIdx);
		result[2] = cities.get(cityIdx);
		result[3] = states.get(cityIdx);

		Date date = getDateWithOffset(0);
		result[4] = df.get().format(date);

		result[5] = genAccountNumber(fNameIdx, lNameIdx, streetIdx, cityIdx, accountIdx);

		result[0] = result[0] + " ".repeat(41 - result[0].length());

		for (int i = 1; i < 4; i++) {
			result[i] = pad(result[i], 20, true);
		}
		result[4] = pad(result[4], 20, false);
		result[5] = pad(result[5], 22, false);		
	}
			
	protected void fillUserInfo(String[] result, Random rndSeq) {

		int fNameIdx = (int) (Math.round(rndSeq.nextDouble() * (firstNames.size() - 1)));
		int lNameIdx = (int) (Math.round(rndSeq.nextDouble() * (lastNames.size() - 1)));
		int streetIdx = (int) (Math.round(rndSeq.nextDouble() * (streets.size() - 1)));
		int cityIdx = (int) Math.round(rndSeq.nextDouble() * (cities.size() - 1));	

		fillUserInfo(result, fNameIdx, lNameIdx, streetIdx, cityIdx, 1);
	}
	
	protected double getRandomAmount(Random rndSeq) {
		return (rndSeq.nextDouble() * Math.pow(10, 5)) / 100;
	}

	protected String getFormatedRandomAmount(double amount, int size) {
		String formattedValue = NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(amount);
		return pad(formattedValue, size, false);
	}

	protected String pad(String v, int size, boolean left) {
		if (v.length() > size) {
			v = v.substring(0, size - 1);
		}
		if (left) {
			return v + " ".repeat(size - v.length());
		} else {
			return " ".repeat(size - v.length()) + v;
		}
	}

	protected void fillOperations(String[] result, Random rndSeq) {
		int idx = 6;

		// init month from the same date as the statement!
		result[idx] = pad(result[4].trim().substring(0, 3), 5, false);
		idx++;

		double total = getRandomAmount(rndSeq) * 30;
		result[idx] = getFormatedRandomAmount(total, 12);
		idx++;

		String opName = "";
		for (int i = 1; i < 15; i++) {
			opName = companies.get((int) Math.round(rndSeq.nextDouble() * (companies.size() - 1)));
			result[idx] = pad(opName, 30, true);
			idx++;
			double op = getRandomAmount(rndSeq);
			result[idx] = getFormatedRandomAmount(op, 12);
			total = total - op;
			idx++;
		}

		result[idx] = getFormatedRandomAmount(total, 12);
	}
}
