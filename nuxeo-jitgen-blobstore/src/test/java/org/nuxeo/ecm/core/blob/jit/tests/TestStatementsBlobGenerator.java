package org.nuxeo.ecm.core.blob.jit.tests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Random;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.ITextNXBankStatementGenerator;

public class TestStatementsBlobGenerator {

	
	@Test
	public void canGeneratePredictableBlobs() throws Exception {
		
		StatementsBlobGenerator bg = new StatementsBlobGenerator();
		bg.initGenerator();			
		
		Long userSeed = new Random().nextLong();
		Long dataSeed = new Random().nextLong();
		Integer month = new Random().nextInt(48);
		
		String key = bg.computeKey(userSeed, dataSeed, month);
		
		String[] meta = bg.getMetaDataForBlobKey(key);

		// verify mapping
		Map<String, String> map = bg.getMetaDataKey(key);
		for (String k : map.keySet()) {
			// check that tags have been cleaned up
			assertFalse(k.contains("#"));
		}
		assertTrue(map.containsKey("ACCOUNTID"));
		assertTrue(map.containsKey("NAME"));
		
		for (int i = 0 ; i < meta.length-1; i++) {
			assertTrue(map.values().contains(meta[i]));
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();			
		StatementMeta smeta= bg.getStream(key, out);		
		
		byte[] pdfData = out.toByteArray();
		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(new ByteArrayInputStream(pdfData)));

		assertTrue(txt.contains(meta[0]));
		assertTrue(txt.contains(smeta.getKeys()[0]));
		
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(pdfData);
	    byte[] digest1 = md.digest();
	    
	    String hexd1= ITextNXBankStatementGenerator.toHexString(digest1);
	    
	    out = new ByteArrayOutputStream();			
		bg.getStream(key, out);		
		
		byte[] pdfData2 = out.toByteArray();		
		String txt2 = stripper.getText(PDDocument.load(new ByteArrayInputStream(pdfData2)));

		assertEquals(txt, txt2);
		assertEquals(pdfData.length, pdfData2.length);
		
		md.reset();
		md.update(pdfData2);
	    byte[] digest2 = md.digest();

	    String hexd2= ITextNXBankStatementGenerator.toHexString(digest2);
	    	    
	    //assertEquals(hexd1, hexd2);		
	}	
}
