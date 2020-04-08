package org.nuxeo.data.gen.tests;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;
import org.nuxeo.data.gen.meta.IdentityIndex;
import org.nuxeo.data.gen.meta.LongCodec;

public class TestLongCodec {
	
	public void testEncodeDecodeRnd() {
		Random rnd = new Random();
		
		int firstNameIdx = rnd.nextInt(LongCodec.FNAME_MAX); 
		int lastNameIdx= rnd.nextInt(LongCodec.LNAME_MAX);
		int streetIdx= rnd.nextInt(LongCodec.STREET_MAX);
		int cityIdx= rnd.nextInt(LongCodec.CITIES_MAX);
		int accountIdx= rnd.nextInt(LongCodec.ACCOUNT_MAX);

		Long key = LongCodec.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
		
		IdentityIndex idx = LongCodec.decode(key);

		assertEquals(accountIdx, idx.getAccountIdx());				
		assertEquals(firstNameIdx, idx.getFirstNameIdx());
		assertEquals(streetIdx, idx.getStreetIdx());
		assertEquals(cityIdx, idx.getCityIdx());
		assertEquals(lastNameIdx, idx.getLastNameIdx());
	}

	public void testEncodeDecodeMin() {
		int firstNameIdx = 0; 
		int lastNameIdx= 0;
		int streetIdx= 0;
		int cityIdx= 0;
		int accountIdx= 0;

		Long key = LongCodec.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
		
		IdentityIndex idx = LongCodec.decode(key);

		assertEquals(accountIdx, idx.getAccountIdx());				
		assertEquals(firstNameIdx, idx.getFirstNameIdx());
		assertEquals(streetIdx, idx.getStreetIdx());
		assertEquals(cityIdx, idx.getCityIdx());
		assertEquals(lastNameIdx, idx.getLastNameIdx());
	}

	public void testEncodeDecodeMax() {
		
		int firstNameIdx = LongCodec.FNAME_MAX-1; 
		int lastNameIdx= LongCodec.LNAME_MAX-1;
		int streetIdx= LongCodec.STREET_MAX-1;
		int cityIdx= LongCodec.CITIES_MAX-1;
		int accountIdx= LongCodec.ACCOUNT_MAX-1;

		Long key = LongCodec.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
		
		IdentityIndex idx = LongCodec.decode(key);

		assertEquals(accountIdx, idx.getAccountIdx());				
		assertEquals(firstNameIdx, idx.getFirstNameIdx());
		assertEquals(streetIdx, idx.getStreetIdx());
		assertEquals(cityIdx, idx.getCityIdx());
		assertEquals(lastNameIdx, idx.getLastNameIdx());
	}


	@Test
	public void testEncodeDecodeMultipleTimes() {
		testEncodeDecodeMin();
		testEncodeDecodeMax();
		for (int i = 0; i < 100000; i++) {
			testEncodeDecodeRnd();
		}
	}
	
	
	
}
