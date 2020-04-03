package org.nuxeo.ecm.core.blob.jit.rnd;

import java.io.InputStream;
import java.util.Random;

import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;

public class AccountHelper {

	public static final Long DEFAULT_SEED = 2020L;

	protected static AccountHelper instance;

	protected final Random masterSequence;

	protected RandomDataGenerator rnd;
	
	public AccountHelper(Long seed) {
		masterSequence = new Random(seed);
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void init() throws Exception {
		rnd = new RandomDataGenerator(false, true);
		InputStream csv = StatementsBlobGenerator.class.getResourceAsStream("/data.csv");
		rnd.init(csv);
		masterSequence.nextLong();
	}
	
	public synchronized static AccountHelper instance() {
		if (instance==null) {
			instance = new AccountHelper(DEFAULT_SEED);
		}		
		return instance;
	}
	
	public String getNextId() {
		String[] data = rnd.generate(masterSequence.nextLong(),0L,0);
		return data[5];
	}
}
