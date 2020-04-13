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

import java.io.InputStream;
import java.io.OutputStream;

import org.nuxeo.data.gen.meta.FormatUtils;
import org.nuxeo.data.gen.pdf.PDFFileGenerator;
import org.nuxeo.data.gen.pdf.StatementMeta;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

public class ITextIDGenerator extends ITextNXBankStatementGenerator implements PDFFileGenerator {

	protected ImageData head;
	
	protected int keySize;
	
	public String getName() {
		return "Template based generation with Index pre-processing using iText";
	}

	public void setPicture(InputStream in) throws Exception {
		if (in != null) {
			head = ImageDataFactory.create(in.readAllBytes());
		}
	}
	
	
	public void init(InputStream pdf, String[] keys) throws Exception {
		super.init(pdf, keys);
		computeDigest=false;
		keySize=keys.length;
	}

	public StatementMeta generate(OutputStream buffer, String[] tokens) throws Exception {
		
		String[] extendedTokens = new String[keySize];		
		System.arraycopy(tokens, 0, extendedTokens, 0, tokens.length-1);
		
		//extendedTokens[7]="YO";
		extendedTokens[6] = FormatUtils.pad(tokens[3].trim().toUpperCase()+ "  ID", 25, true);
		
		return super.generate(buffer, extendedTokens);
	}
	
	protected void postProcessDoc(PdfDocument doc) throws Exception {
		PdfPage page = doc.getFirstPage();
	    PdfCanvas canvas = new PdfCanvas(page);
	    Rectangle rect = new Rectangle(50, 58);
	    rect.setX(18f);
	    rect.setY(18.5f);
	    canvas.addImage(head, rect, true);	    
		return;
	}


}
