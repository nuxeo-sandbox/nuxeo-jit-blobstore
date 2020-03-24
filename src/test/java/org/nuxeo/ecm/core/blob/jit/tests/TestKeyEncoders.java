package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;
import org.nuxeo.ecm.core.blob.jit.rnd.key.AsciiKeyCodec;
import org.nuxeo.ecm.core.blob.jit.rnd.key.DummyKeyCodec;
import org.nuxeo.ecm.core.blob.jit.rnd.key.KeyCodec;

public class TestKeyEncoders {
	
	protected static final int SHORT_INT_MAXVALUE = 200;
	
	@Test
	public void shouldEncodeLongAndShortIntegerAsAscii() {
		
		String max = AsciiKeyCodec.encode(Long.MAX_VALUE);		
		//System.out.println(max);
		assertEquals(Long.MAX_VALUE, AsciiKeyCodec.decode(max));
		
		String min = AsciiKeyCodec.encode(Long.MIN_VALUE);		
		//System.out.println(min);
		assertEquals(Long.MIN_VALUE, AsciiKeyCodec.decode(min));
		
		
		
		max = AsciiKeyCodec.encode(SHORT_INT_MAXVALUE);		
		//System.out.println(max);
		assertEquals(SHORT_INT_MAXVALUE, AsciiKeyCodec.decode(max));
		
		min = AsciiKeyCodec.encode(-SHORT_INT_MAXVALUE);		
		//System.out.println(min);
		assertEquals(-SHORT_INT_MAXVALUE, AsciiKeyCodec.decode(min));
		
		
		Random rnd = new Random();
		for (int x = 0; x < 1000; x++) {
			long l = rnd.nextLong();
			String k = AsciiKeyCodec.encode(l);
			//System.out.println(k);
			assertEquals(l, AsciiKeyCodec.decode(k));
			
			int i = rnd.nextInt(SHORT_INT_MAXVALUE);
			k = AsciiKeyCodec.encode(i);
			//System.out.println(k);
			assertEquals(i, AsciiKeyCodec.decode(k));						
		}
	}

	@Test
	public void shouldEncodeDecodeAsNumber() {
		testSeedEncoders(new DummyKeyCodec());
	}
	
	@Test
	public void shouldEncodeDecodeAsAscii() {
		testSeedEncoders(new AsciiKeyCodec());
	}
	
	protected void testSeedEncoders(KeyCodec codec) {
				
		checkEncodeDecode(codec, Long.MAX_VALUE, Long.MAX_VALUE, SHORT_INT_MAXVALUE);
		checkEncodeDecode(codec, Long.MIN_VALUE, Long.MIN_VALUE, SHORT_INT_MAXVALUE);
		checkEncodeDecode(codec, 0L,0L,0);
		
		Random rnd = new Random();
		for (int x = 0; x < 1000; x++) {
			long s1 = rnd.nextLong();
			long s2 = rnd.nextLong();
			int i = rnd.nextInt(SHORT_INT_MAXVALUE);
			checkEncodeDecode(codec, s1,s2,i);	
		}				
	}
	
	protected void checkEncodeDecode(KeyCodec codec, Long seed1, Long seed2, Integer m) {
		String key = codec.encodeSeeds(seed1,seed2, m);
		Long[] seeds = codec.decodeSeeds(key);
		assertEquals(seed1, seeds[0]);
		assertEquals(seed2, seeds[1]);
		assertEquals(m.intValue(), seeds[2].intValue());
	}
}
