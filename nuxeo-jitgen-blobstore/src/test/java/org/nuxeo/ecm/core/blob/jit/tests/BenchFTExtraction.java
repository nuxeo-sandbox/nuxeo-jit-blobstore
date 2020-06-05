package org.nuxeo.ecm.core.blob.jit.tests;

import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.jit.es.PDFBoxFTExtractor;
import org.nuxeo.ecm.core.blob.jit.es.PDFBoxFTExtractor2;
import org.nuxeo.ecm.core.blob.jit.es.PDFFulltextExtractor;
import org.nuxeo.ecm.core.blob.jit.es.iTextFTExtractor;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

public class BenchFTExtraction {

	protected static final int NB_CALLS = 5000;
	protected static final int NB_THREADS = 5;
	protected Random rnd = new Random();
	protected StatementsBlobGenerator bg;
	
	protected String getText(InputStream is) throws Exception {
		return getText(new PDFTextStripper(), is);
	}
	
	protected String getText2(InputStream is) throws Exception {
		PdfReader reader = new PdfReader(is);
        PdfDocument pdf = new PdfDocument(reader);
        try {
        	return PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
        } finally {
			pdf.close();
		}
	}
	protected String getText(PDFTextStripper stripper, InputStream is) throws Exception {
		
		PDDocument doc = PDDocument.load(is);
		try {
			return stripper.getText(doc);
		} finally {
			doc.close();
		}		
	}

	
				
	@Before
	public void setup() throws Exception {
		bg = new StatementsBlobGenerator();
		bg.initGenerator();	
	}	
	
	protected String getBlobKey() {
		Long userSeed = rnd.nextLong();
		Long dataSeed = rnd.nextLong();
		Integer month = rnd.nextInt(48);		
		return bg.computeKey(userSeed, dataSeed, month);
	}

	@Test
	public void benchmarkPDFBoxTextExtract() throws Exception {
		benchmarkTextExtraction(PDFBoxFTExtractor.class);		
	}

	@Test
	public void benchmarkiTextTextExtract() throws Exception {
		benchmarkTextExtraction(iTextFTExtractor.class);		
	}
	
	@Test
	public void benchmarkPDFBoxTextExtract2() throws Exception {
		benchmarkTextExtraction(PDFBoxFTExtractor2.class);		
	}

	class NoExtract implements PDFFulltextExtractor {

		public NoExtract() {
			
		}
		
		@Override
		public String getText(InputStream pdf) throws Exception {			
			return "";
		}
		
	}
	@Test
	public void benchmarkNoTextExtract() throws Exception {
		
		benchmarkTextExtraction(null);		
	}

	
	protected void benchmarkTextExtraction(Class<? extends PDFFulltextExtractor> txt) throws Exception {
		 
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NB_THREADS);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();
		
		long t0 = System.currentTimeMillis();
		
		final class Task implements Runnable {
			@Override
			public void run() {
				try {
					PDFFulltextExtractor extractor=null;
					if (txt!=null) {
						extractor = txt.getDeclaredConstructor().newInstance();	
					}					
					for (int i = 0; i < NB_CALLS; i++) {
						String key = getBlobKey();
						InputStream is = bg.getStream(key);
						if (extractor!=null) {
							String t = extractor.getText(is);
						}
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
		Double throughput = (counter.get() * 1.0 / (t1 - t0))* 1000 ;
		if (txt!=null) {
			System.out.println(txt.getSimpleName() + " throughput:" + throughput);
		} else {
			System.out.println("Raw throughput:" + throughput);
		}
	}

}
