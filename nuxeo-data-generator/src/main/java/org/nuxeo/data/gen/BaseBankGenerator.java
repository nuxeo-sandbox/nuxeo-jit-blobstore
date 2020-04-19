package org.nuxeo.data.gen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.apache.commons.io.output.CountingOutputStream;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.filter.PDFOutputFilter;

import com.itextpdf.kernel.pdf.PdfDocument;

public abstract class BaseBankGenerator {

	public boolean computeDigest = false;

	protected PDFOutputFilter filter;
	
	public void setFilter(PDFOutputFilter filter) {
		this.filter=filter;
	}
	
	protected String getFileName(String[] tokens, String extension) {
		
		return getType() + "-" + tokens[5].trim() + extension;
	}
	
	protected abstract void postProcessDoc(PdfDocument doc) throws Exception;
	

	public static String toHexString(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();

		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}

		return hexString.toString();
	}

	public abstract String getType();
	
	protected abstract void doGenerate(OutputStream buffer, String[] tokens) throws Exception;
	
	public StatementMeta generate(OutputStream buffer, String[] tokens) throws Exception {

		DigestOutputStream db = null;

		CountingOutputStream cout = null;
		ByteArrayOutputStream tmpBuffer = null;
		
		if (computeDigest) {
			db = new DigestOutputStream(buffer, MessageDigest.getInstance("MD5"));
			cout = new CountingOutputStream(db);
		} else {
			cout = new CountingOutputStream(buffer);
		}

		if (filter!=null) {
			tmpBuffer = new ByteArrayOutputStream();
			doGenerate(tmpBuffer, tokens);
		} else {
			doGenerate(cout, tokens);
		}
		
		String extension=getDefaultExtension();		
		if (filter!=null) {
			if (tmpBuffer==null) {
				System.out.println("WTF!!!");
			}
			filter.render(new ByteArrayInputStream(tmpBuffer.toByteArray()), cout);
			extension = filter.getFileExtension();
		} 
		
		String fileName = getFileName(tokens, extension);
		long fileSize = cout.getByteCount();
		String md5 = "not-computed";

		if (db != null) {
			byte[] digest = db.getMessageDigest().digest();
			md5 = toHexString(digest);
		}
		return new StatementMeta(md5, fileName, fileSize, tokens);
	}

	protected abstract String getDefaultExtension();

}
