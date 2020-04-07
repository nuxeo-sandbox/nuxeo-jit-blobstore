package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;
import org.nuxeo.ecm.core.blob.jit.rnd.key.LongCodec;

public class TestLongCodec {
	
	public void testEncodeDecodeRnd() {
		Random rnd = new Random();
		LongCodec lc = new LongCodec();
		
		int firstNameIdx = rnd.nextInt(LongCodec.FNAME_MAX); 
		int lastNameIdx= rnd.nextInt(LongCodec.LNAME_MAX);
		int streetIdx= rnd.nextInt(LongCodec.STREET_MAX);
		int cityIdx= rnd.nextInt(LongCodec.CITIES_MAX);
		int accountIdx= rnd.nextInt(LongCodec.ACCOUNT_MAX);

		Long key = lc.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
		
		LongCodec.Index idx = lc.decode(key);

		assertEquals(accountIdx, idx.accountIdx);				
		assertEquals(firstNameIdx, idx.firstNameIdx);
		assertEquals(streetIdx, idx.streetIdx);
		assertEquals(cityIdx, idx.cityIdx);
		assertEquals(lastNameIdx, idx.lastNameIdx);
	}

	public void testEncodeDecodeMin() {
		LongCodec lc = new LongCodec();		
		int firstNameIdx = 0; 
		int lastNameIdx= 0;
		int streetIdx= 0;
		int cityIdx= 0;
		int accountIdx= 0;

		Long key = lc.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
		
		LongCodec.Index idx = lc.decode(key);

		assertEquals(accountIdx, idx.accountIdx);				
		assertEquals(firstNameIdx, idx.firstNameIdx);
		assertEquals(streetIdx, idx.streetIdx);
		assertEquals(cityIdx, idx.cityIdx);
		assertEquals(lastNameIdx, idx.lastNameIdx);
	}

	public void testEncodeDecodeMax() {
		LongCodec lc = new LongCodec();
		
		int firstNameIdx = LongCodec.FNAME_MAX-1; 
		int lastNameIdx= LongCodec.LNAME_MAX-1;
		int streetIdx= LongCodec.STREET_MAX-1;
		int cityIdx= LongCodec.CITIES_MAX-1;
		int accountIdx= LongCodec.ACCOUNT_MAX-1;

		Long key = lc.encode(firstNameIdx, lastNameIdx, streetIdx, cityIdx, accountIdx);
		
		LongCodec.Index idx = lc.decode(key);

		assertEquals(accountIdx, idx.accountIdx);				
		assertEquals(firstNameIdx, idx.firstNameIdx);
		assertEquals(streetIdx, idx.streetIdx);
		assertEquals(cityIdx, idx.cityIdx);
		assertEquals(lastNameIdx, idx.lastNameIdx);
	}


	@Test
	public void testEncodeDecodeMultipleTimes() {
		testEncodeDecodeMin();
		testEncodeDecodeMax();
		for (int i = 0; i < 10000; i++) {
			testEncodeDecodeRnd();
		}
	}
	
	
	
}
