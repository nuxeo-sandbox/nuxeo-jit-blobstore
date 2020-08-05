package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.thumbnail.StatementThumbnailFactory;
import org.nuxeo.importer.stream.jit.USStateHelper;

public class BenchPDF2Thumb {

	protected static final int NB_CALLS = 100;
	protected static final int NB_THREADS = 5;

	protected byte[] getPDF() throws Exception {
		InputStream sample = StatementsBlobGenerator.class.getResourceAsStream("/sample.pdf");
		return IOUtils.toByteArray(sample);
	}

	@Test
	public void canGenerateThumb() throws Exception {

		Blob blob = StatementThumbnailFactory.computeThumb(getPDF());

		assertNotNull(blob);

//		 Path f = Files.createTempFile("thumb", ".png");
//		 blob.transferTo(f.toFile());
//		 System.out.println(f);
	}

	@Test
	public void benchmarkPDF2PNGSpeed() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();

		long t0 = System.currentTimeMillis();
		byte[] pdf = getPDF();
		final class Task implements Runnable {
			@Override
			public void run() {
				try {
					for (int i = 0; i < NB_CALLS; i++) {
						StatementThumbnailFactory.computeThumb(pdf);
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
		Double throughput = counter.get() * 1.0 / ((t1 - t0) / 1000);
		System.out.println("Throughput:" + throughput);
	}

}
