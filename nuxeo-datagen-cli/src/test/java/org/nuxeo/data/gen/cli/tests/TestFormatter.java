package org.nuxeo.data.gen.cli.tests;

import org.junit.Test;
import org.nuxeo.data.gen.cli.ResponseHelper;

public class TestFormatter {

	
	@Test
	public void testFormat() throws Exception {
		
		String message ="{\"entity-type\":\"string\",\"value\":\"{\\\"elapsed\\\":60.106,\\\"committed\\\":26,\\\"failures\\\":0,\\\"consumers\\\":8,\\\"throughput\\\":0.4325691278740891}\"}";
		
		System.out.println(ResponseHelper.formatAsString(message));

		System.out.println(ResponseHelper.formatAsMap(message));
		
	}
}
