package org.nuxeo.data.gen.tests;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.nuxeo.data.gen.meta.FormatUtils;
import org.nuxeo.data.gen.meta.RandomDataGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextIDGenerator;
import org.nuxeo.data.gen.pdf.itext.ITextIDTemplateCreator;

import com.google.common.io.Files;

public class TestIDCards {

	protected RandomDataGenerator getRndGenerator(boolean generateOperations) throws Exception {
		// Data Generator
		RandomDataGenerator rnd = new RandomDataGenerator(generateOperations, true);		
		InputStream csv = TestPDFGeneration.class.getResourceAsStream("/data.csv");
		rnd.init(csv);
		return rnd;
	}
	
	@Test
	public void generateIDCardFromCSV() throws Exception {

		
		ITextIDTemplateCreator templateGen = new ITextIDTemplateCreator();		
		InputStream bg = ITextIDTemplateCreator.class.getResourceAsStream("/id-back.jpeg");
		templateGen.init(bg);
		String[] keys = templateGen.getKeys();
		for (String k: keys) {
			System.out.println(k);			
		}
		
		
		ByteArrayOutputStream templateOut = new ByteArrayOutputStream();
		templateGen.generate(templateOut);
		byte[] templateData =  templateOut.toByteArray();

		
		ITextIDGenerator gen = new ITextIDGenerator();
		gen.init(new ByteArrayInputStream(templateData), keys);
		gen.computeDigest = true;
		
		gen.setPicture(ITextIDTemplateCreator.class.getResourceAsStream("/jexo.jpeg"));
	
		
		
		InputStream csv = this.getClass().getResourceAsStream("/letters.csv");		
		Scanner scanner = new Scanner(csv);
		
		String[] pdfKeys= new String[7];
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			String[] parts = line.split(",");
			
			
			pdfKeys[0]=FormatUtils.pad(parts[3],41, false);
			pdfKeys[1]=FormatUtils.pad(parts[4],20, false);
			pdfKeys[2]=FormatUtils.pad(parts[5],20, false);
			pdfKeys[3]=FormatUtils.pad(parts[6],20, false);
			//pdfKeys[4]=parts[3];
			pdfKeys[5]=FormatUtils.pad(parts[8],20, false);	
			
			
			ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
			//FileOutputStream pdfOut = new FileOutputStream(pdf);
			StatementMeta meta = gen.generate(pdfOut, pdfKeys);
			assertTrue(meta.getFileName().endsWith("pdf"));

			byte[] pdf = pdfOut.toByteArray();
			assertTrue(meta.getDigest().equalsIgnoreCase(DigestUtils.md5Hex(pdf)));
			
			
			File pdfFile = File.createTempFile("idcard", ".pdf");
			Files.write(pdf, pdfFile);			
			System.out.println(pdfFile.getAbsolutePath());

		}

		scanner.close();

		}
}
