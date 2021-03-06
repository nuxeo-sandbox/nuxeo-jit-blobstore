package org.nuxeo.data.gen.tests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextIDGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextIDTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankStatementGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankTemplateCreator2;
import org.nuxeo.data.gen.pdf.itext.filter.JpegFilter;
import org.nuxeo.data.gen.pdf.itext.filter.PDFOutputFilter;

public class TestPDFGeneration {
	
	protected byte[] getTemplate(ITextNXBankTemplateCreator templateGen) throws Exception {

		// Generate the template
		InputStream logo = TestPDFGeneration.class.getResourceAsStream("/NxBank3.png");
		templateGen.init(logo);

		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		return templateOut.toByteArray();
	}

	protected RandomDataGenerator getRndGenerator(boolean generateOperations) throws Exception {
		// Data Generator
		RandomDataGenerator rnd = new RandomDataGenerator(generateOperations, true);		
		InputStream csv = TestPDFGeneration.class.getResourceAsStream("/data.csv");
		rnd.init(csv);
		return rnd;
	}

	protected File dumpPDF(byte[] pdf) throws Exception {
		return dumpFile(pdf, ".pdf");
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
	public void canGenerateRandomData() throws Exception {

		RandomDataGenerator rnd = getRndGenerator(false);

		String[] data = rnd.generate();
		String[] keys = new ITextNXBankTemplateCreator().getKeys();

		// ID appended to data
		assertEquals(keys.length+1, data.length);
		for (int i = 0; i < keys.length; i++) {
			assertFalse(data[i].isEmpty());
		}
		
		rnd = getRndGenerator(true);
		String[] data2 = rnd.generate();
		assertTrue(data2.length> data.length);
		for (int i = 0; i < data2.length; i++) {
			assertFalse(data2[i].isEmpty());
			//System.out.println(data2[i]);
		}
	}

	@Test
	public void canGenerateTemplate() throws Exception {

		ITextNXBankTemplateCreator templateGen = new ITextNXBankTemplateCreator();
				
		assertEquals("#NAME-----------------------------------#", ITextNXBankTemplateCreator.mkTag("NAME", 41));
		assertEquals("#STREET------------#", ITextNXBankTemplateCreator.mkTag("STREET", 20));
		
		byte[] templateData = getTemplate(templateGen);
		String[] keys = templateGen.getKeys();
		
		assertTrue(templateData.length > 0);

		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(new ByteArrayInputStream(templateData)));

		assertTrue(txt.contains(ITextNXBankTemplateCreator.ACCOUNT_LABEL));
		for (int i = 0; i < keys.length; i++) {
			assertTrue(txt.contains(keys[i]));
		}		
	}

	@Test
	public void canGenerateTemplate2() throws Exception {
		
		ITextNXBankTemplateCreator2 templateGen = new ITextNXBankTemplateCreator2();
		byte[] templateData = getTemplate(templateGen);
		String[] keys = templateGen.getKeys();
		
		assertTrue(templateData.length > 0);

		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(new ByteArrayInputStream(templateData)));

		assertTrue(txt.contains(ITextNXBankTemplateCreator.ACCOUNT_LABEL));
		for (int i = 0; i < keys.length; i++) {
			assertTrue(txt.contains(keys[i]));
		}		
	}

	@Test
	public void canGenerateIDFromTemplate() throws Exception {
		
		ITextIDTemplateCreator templateGen = new ITextIDTemplateCreator();		
		InputStream bg = ITextIDTemplateCreator.class.getResourceAsStream("/id-back.jpeg");
		templateGen.init(bg);
		String[] keys = templateGen.getKeys();
		
		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData =  templateOut.toByteArray();
		
		RandomDataGenerator rnd = getRndGenerator(false);

		ITextIDGenerator gen = new ITextIDGenerator();
		gen.init(new ByteArrayInputStream(templateData), keys);
		gen.computeDigest = true;
		
		gen.setPicture(ITextIDTemplateCreator.class.getResourceAsStream("/jexo.jpeg"));
		
		//Path p = Paths.get("/home/tiry/Pictures/id-faces/small");
		//gen.setPictureFolder(p);				
		
		// PDF GEN
		String[] data = rnd.generate();
		ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
		StatementMeta meta = gen.generate(pdfOut, data);
		assertTrue(meta.getFileName().endsWith("pdf"));

		byte[] pdf = pdfOut.toByteArray();
		assertTrue(meta.getDigest().equalsIgnoreCase(DigestUtils.md5Hex(pdf)));
		//dumpPDF(pdf);
		
		// Jpeg Gen
		ByteArrayOutputStream jpgOut = new ByteArrayOutputStream();
		PDFOutputFilter filter = new JpegFilter();
		filter.setDPI(300);
		gen.setFilter(filter);
		meta = gen.generate(jpgOut, data);
		assertTrue(meta.getFileName().endsWith("jpg"));
		assertTrue(meta.getDigest().equalsIgnoreCase(DigestUtils.md5Hex(jpgOut.toByteArray())));
		
		System.out.println(jpgOut.toByteArray().length/1024);
		dumpFile(jpgOut.toByteArray(), filter.getFileExtension());
		
		// Tiff Gen
		// Tiff gen not possible in java 8
		/*ByteArrayOutputStream tifOut = new ByteArrayOutputStream();
		filter = new TiffFilter();
		filter.setDPI(300);
		gen.setFilter(filter);
		meta = gen.generate(tifOut, data);
		assertTrue(meta.getFileName().endsWith("tif"));
		assertTrue(meta.getDigest().equalsIgnoreCase(DigestUtils.md5Hex(tifOut.toByteArray())));
		*/
		//dumpFile(tifOut.toByteArray(), filter.getFileExtension());
		
	}

	@Test
	public void canGenerateIDTemplate() throws Exception {
		
		ITextIDTemplateCreator templateGen = new ITextIDTemplateCreator();		
		InputStream bg = ITextIDTemplateCreator.class.getResourceAsStream("/id-back.jpeg");
		templateGen.init(bg);

		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData =  templateOut.toByteArray();
		
		assertTrue(templateData.length > 0);

		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(new ByteArrayInputStream(templateData)));

		dumpPDF(templateData);
	}
	
	
	@Test
	public void canGenerateFromTemplate() throws Exception {

		ITextNXBankTemplateCreator templateGen = new ITextNXBankTemplateCreator();
		byte[] templateData = getTemplate(templateGen);
		String[] keys = templateGen.getKeys();
		
		RandomDataGenerator rnd = getRndGenerator(false);

		ITextNXBankStatementGenerator gen = new ITextNXBankStatementGenerator();
		gen.init(new ByteArrayInputStream(templateData), keys);
		gen.computeDigest = true;

		ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
		gen.generate(pdfOut, rnd.generate());

		byte[] pdf = pdfOut.toByteArray();

		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(new ByteArrayInputStream(pdf)));

		assertTrue(txt.contains(ITextNXBankTemplateCreator.ACCOUNT_LABEL));
		for (int i = 0; i < keys.length; i++) {
			assertFalse(txt.contains(keys[i]));
		}

	}
			
	@Test
	public void canGenerateFromTemplate2() throws Exception {

		ITextNXBankTemplateCreator2 templateGen = new ITextNXBankTemplateCreator2();
		byte[] templateData = getTemplate(templateGen);
		String[] keys = templateGen.getKeys();
		
		RandomDataGenerator rnd = getRndGenerator(true);
		
		ITextNXBankStatementGenerator gen = new ITextNXBankStatementGenerator();
		gen.init(new ByteArrayInputStream(templateData), keys);
		gen.computeDigest = true;


		ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
		gen.generate(pdfOut, rnd.generate());

		byte[] pdf = pdfOut.toByteArray();

		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(new ByteArrayInputStream(pdf)));

		assertTrue(txt.contains(ITextNXBankTemplateCreator.ACCOUNT_LABEL));
		for (int i = 0; i < keys.length; i++) {
			assertFalse(txt.contains(keys[i]));
		}
		
		dumpPDF(pdf);

	}

}
