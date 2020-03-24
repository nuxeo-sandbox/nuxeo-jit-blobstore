package org.nuxeo.ecm.core.blob.jit.gen.pdf.itext;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

public class HackedPDFDocument extends PdfDocument {

	public HackedPDFDocument(PdfWriter writer) {
		super(writer);
	}

	public HackedPDFDocument(PdfReader reader, PdfWriter writer) {
		super(reader, writer);
	}
	
	protected void updateXmpMetadata() {
		// NOP
	}

}
