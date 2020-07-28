package org.nuxeo.data.gen.cli.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;
import org.nuxeo.data.gen.cli.KeyBuffer;

public class TestKeyBuffer {

	@Test
	public void testDedup() {

		KeyBuffer kb = new KeyBuffer(5);
		String k1 = UUID.randomUUID().toString();
		String k2 = UUID.randomUUID().toString();
		kb.add(k1);

		assertTrue(kb.contains(k1));
		assertFalse(kb.contains(k2));
		kb.add(k2);

		assertTrue(kb.contains(k1));
		assertTrue(kb.contains(k2));
	}

	@Test
	public void testCycle() {

		int size = 1000;
		KeyBuffer kb = new KeyBuffer(size, 10);
		String[] keys = new String[size + 1];

		for (int i = 0; i <= size; i++) {
			keys[i] = UUID.randomUUID().toString();
			assertFalse(kb.contains(keys[i]));
			kb.add(keys[i]);
			assertTrue(kb.contains(keys[i]));
		}

		assertEquals(size - 9, kb.size());

		assertFalse(kb.contains(keys[0]));
		for (int i = 10; i <= size; i++) {
			assertTrue(kb.contains(keys[i]));
		}
	}

}
