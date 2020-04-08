package org.nuxeo.data.gen.tests;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

public class BenchPDF2Text {

	protected static final int NB_CALLS = 1000;
	protected static final int NB_THREADS = 10;

	@Test
	public void testPDF2TextSpeed() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();

		long t0 = System.currentTimeMillis();
		InputStream sample = BenchPDF2Text.class.getResourceAsStream("/sample.pdf");				
		byte[] pdf = IOUtils.toByteArray(sample);
		
		final class Task implements Runnable {

			@Override
			public void run() {
				
				try {
					PDFTextStripper stripper = new PDFTextStripper();
					for (int i = 0; i < NB_CALLS; i++) {
						PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf));
						String txt = stripper.getText(doc);
						doc.close();
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
