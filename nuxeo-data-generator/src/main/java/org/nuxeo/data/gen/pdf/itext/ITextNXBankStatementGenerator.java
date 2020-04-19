/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 */
package org.nuxeo.data.gen.pdf.itext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.CountingOutputStream;
import org.nuxeo.data.gen.BaseBankGenerator;
import org.nuxeo.data.gen.pdf.PDFFileGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;
import org.nuxeo.data.gen.pdf.itext.filter.PDFOutputFilter;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;

public class ITextNXBankStatementGenerator extends BaseBankGenerator implements PDFFileGenerator {

	protected byte[] template;

	protected Map<Integer, Integer> index = new HashMap<Integer, Integer>();


	public String getName() {
		return "Template based generation with Index pre-processing using iText";
	}

	
	public void init(InputStream pdf, String[] keys) throws Exception {

		template = new byte[pdf.available()];
		pdf.read(template);

		PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(template));
		PdfDocument doc = new PdfDocument(pdfReader);

		PdfPage page = doc.getFirstPage();
		PdfDictionary dict = page.getPdfObject();

		PdfObject object = dict.get(PdfName.Contents);

		if (object instanceof PdfStream) {
			PdfStream stream = (PdfStream) object;
			byte[] data = stream.getBytes();
			String txt = new String(data);
			for (int k = 0; k < keys.length; k++) {
				String key = keys[k];
				int idx = 0;
				do {
					idx = txt.indexOf(key, idx);
					if (idx > 0) {
						index.put(idx, k);
					}
					idx++;
				} while (idx > 0);
			}
		}
		doc.close();
	}
	
	protected void doGenerate(OutputStream buffer, String[] tokens) throws Exception {
		PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(template));

		WriterProperties wp = new WriterProperties();
		wp.setPdfVersion(PdfVersion.PDF_1_4);
		wp.useSmartMode();
		PdfWriter writer=null;

		writer = new PdfWriter(buffer, wp);
		
		PdfDocument doc = new HackedPDFDocument(pdfReader, writer);
		PdfPage page = doc.getFirstPage();
		PdfDictionary dict = page.getPdfObject();
		PdfObject object = dict.get(PdfName.Contents);

		if (object instanceof PdfStream) {
			PdfStream stream = (PdfStream) object;
			byte[] data = stream.getBytes();

			for (Integer idx : index.keySet()) {
				byte[] chunk = tokens[index.get(idx)].getBytes();
				System.arraycopy(chunk, 0, data, idx, chunk.length);
			}
			stream.setData(data);
		}
		
		postProcessDoc(doc);		
		
		doc.close();				
		writer.flush();
		writer.close();
	}
	
		
	protected void postProcessDoc(PdfDocument doc) throws Exception {
		// NOP
		return;
	}
	
	public String getType() {
		return "Statement";
	}

	@Override
	protected String getDefaultExtension() {
		return ".pdf";
	}
}
