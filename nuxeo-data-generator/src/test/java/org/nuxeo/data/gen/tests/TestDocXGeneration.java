package org.nuxeo.data.gen.tests;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.nuxeo.data.gen.BaseBankTemplate;
import org.nuxeo.data.gen.docx.DocXLetterGenerator;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextIDGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextIDTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.filter.JpegFilter;
import org.nuxeo.data.gen.pdf.itext.filter.PDFOutputFilter;

public class TestDocXGeneration {
	
	protected byte[] getTemplate(ITextNXBankTemplateCreator templateGen) throws Exception {

		// Generate the template
		InputStream logo = TestDocXGeneration.class.getResourceAsStream("/NxBank3.png");
		templateGen.init(logo);

		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		return templateOut.toByteArray();
	}

	protected RandomDataGenerator getRndGenerator(boolean generateOperations) throws Exception {
		// Data Generator
		RandomDataGenerator rnd = new RandomDataGenerator(generateOperations, true);		
		InputStream csv = TestDocXGeneration.class.getResourceAsStream("/data.csv");
		rnd.init(csv);
		return rnd;
	}


	protected File dumpFile(byte[] pdf, String extension) throws Exception {
		File tmp = File.createTempFile("tmp", extension);
		Files.copy(new ByteArrayInputStream(pdf), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
		System.out.println(tmp.getAbsolutePath());
		return tmp;
	}

	protected String printData(String[] data) {

		StringBuffer sb = new StringBuffer();
		
		for (String entry:data) {
			sb.append(entry);
			sb.append(" -- ");			
		}		
		return sb.toString();
	}
	
	
	@Test
	public void canGenerateDocXLetter() throws Exception {

		RandomDataGenerator rnd = getRndGenerator(false);
		InputStream docXTemplate = TestDocXGeneration.class.getResourceAsStream("/new_account.docx");
		DocXLetterGenerator docxGen = new DocXLetterGenerator();
		docxGen.init(docXTemplate, BaseBankTemplate._keys);		
		
		ByteArrayOutputStream docxOut = new ByteArrayOutputStream();

		StatementMeta meta = docxGen.generate(docxOut, rnd.generate());
		
		System.out.println(meta.getFileName());
		byte[] docX = docxOut.toByteArray();
		assertTrue(meta.getDigest().equalsIgnoreCase(DigestUtils.md5Hex(docX)));
		
		dumpFile(docxOut.toByteArray(), ".docx");
	}

	
	protected static final int NB_CALLS = 5000;
	protected static final int NB_THREADS = 6;

	@Test
	public void benchDocXGen() throws Exception {

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NB_THREADS);
		executor.prestartAllCoreThreads();
		AtomicInteger counter = new AtomicInteger();

		
		RandomDataGenerator rnd = getRndGenerator(false);
		InputStream docXTemplate = TestDocXGeneration.class.getResourceAsStream("/new_account.docx");
		DocXLetterGenerator docxGen = new DocXLetterGenerator();
		docxGen.init(docXTemplate, BaseBankTemplate._keys);		
		
		long t0 = System.currentTimeMillis();
		
		final class Task implements Runnable {

			@Override
			public void run() {
				
				try {
					for (int i = 0; i < NB_CALLS; i++) {

						String[] data = rnd.generate();
						ByteArrayOutputStream docxOut = new ByteArrayOutputStream();
						StatementMeta meta = docxGen.generate(docxOut, data);

						byte[] docX = docxOut.toByteArray();
						assertTrue(meta.getDigest().equalsIgnoreCase(DigestUtils.md5Hex(docX)));
												
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
