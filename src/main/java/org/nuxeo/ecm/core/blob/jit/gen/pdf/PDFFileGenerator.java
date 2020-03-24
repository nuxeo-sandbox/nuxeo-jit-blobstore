package org.nuxeo.ecm.core.blob.jit.gen.pdf;

import java.io.InputStream;
import java.io.OutputStream;

import org.nuxeo.ecm.core.blob.jit.gen.SmtMeta;

public interface PDFFileGenerator {

	void init(InputStream pdfTemplate, String[] keys) throws Exception;

	SmtMeta generate(OutputStream pdf, String[] tokens) throws Exception;

}
