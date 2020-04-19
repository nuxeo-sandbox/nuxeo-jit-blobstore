package org.nuxeo.data.gen.tests;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextIDGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextIDTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.filter.JpegFilter;
import org.nuxeo.data.gen.pdf.itext.filter.PDFOutputFilter;

public class BenchJpegGen {

	protected static final int NB_CALLS = 100;
	protected static final int NB_THREADS = 6;

	@Test
	public void testJPegSpeed() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NB_THREADS);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();

		
		
		ITextIDTemplateCreator templateGen = new ITextIDTemplateCreator();		
		InputStream bg = ITextIDTemplateCreator.class.getResourceAsStream("/id-back.jpeg");
		templateGen.init(bg);
		String[] keys = templateGen.getKeys();
		
		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData =  templateOut.toByteArray();
		
		RandomDataGenerator rnd = new RandomDataGenerator(false, true);		
		InputStream csv = TestPDFGeneration.class.getResourceAsStream("/data.csv");
		rnd.init(csv);
		
		ITextIDGenerator gen = new ITextIDGenerator();
		gen.init(new ByteArrayInputStream(templateData), keys);
		gen.computeDigest = true;		
		gen.setPicture(ITextIDTemplateCreator.class.getResourceAsStream("/jexo.jpeg"));

		PDFOutputFilter filter = new JpegFilter();
		filter.setDPI(300);
		gen.setFilter(filter);

		long t0 = System.currentTimeMillis();
		
		final class Task implements Runnable {

			@Override
			public void run() {
				
				try {
					for (int i = 0; i < NB_CALLS; i++) {

						String[] data = rnd.generate();
						
						// Jpeg Gen
						ByteArrayOutputStream jpgOut = new ByteArrayOutputStream();
						StatementMeta meta = gen.generate(jpgOut, data);
						
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
