package org.nuxeo.importer.stream.jit;

import java.util.Date;

import io.netty.util.internal.ThreadLocalRandom;

public class DateHelper {

	protected static final long jan2015 = 1420088400000L;
	protected static final long dec2015 = 1448946000000L;

	public static Date getRandomDatein2015() {
		return new Date(ThreadLocalRandom.current().nextLong(jan2015, dec2015));
	}
}
