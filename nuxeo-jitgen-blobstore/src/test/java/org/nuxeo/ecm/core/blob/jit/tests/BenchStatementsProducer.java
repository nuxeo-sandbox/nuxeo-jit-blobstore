package org.nuxeo.ecm.core.blob.jit.tests;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.importer.stream.jit.StatementDocumentMessageProducer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
public class BenchStatementsProducer {

	protected static final int NB_CALLS = 100000;
	protected static final int NB_THREADS = 16;
	
	@Test
	public void benchmarkPDF2PNGSpeed() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NB_THREADS);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();
		AtomicInteger ids = new AtomicInteger();
		SequenceGenerator sequenceGen = new SequenceGenerator(SequenceGenerator.DEFAULT_ACCOUNT_SEED, 60);
		
		long t0 = System.currentTimeMillis();
		final class Task implements Runnable {
			@Override
			public void run() {
				
				StatementDocumentMessageProducer producer = new StatementDocumentMessageProducer(sequenceGen, ids.incrementAndGet(), NB_CALLS, 60); 				
				try {
					while (producer.hasNext()) {
						producer.next();
						counter.incrementAndGet();
					}					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		for (int i = 0; i < NB_THREADS; i++) {
			executor.execute(new Task());
		}

		executor.shutdown();
		boolean finished = executor.awaitTermination(3 * 60, TimeUnit.SECONDS);
		if (!finished) {
			System.out.println("Timeout after " + counter.get() + " generations");
		}

		long t1 = System.currentTimeMillis();
		Double throughput = counter.get() * 1.0 / (t1 - t0);
		throughput = throughput*1000;
		System.out.println("Throughput:" + throughput);
	}


}
