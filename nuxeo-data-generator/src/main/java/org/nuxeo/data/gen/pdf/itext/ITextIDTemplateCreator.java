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
import java.util.ArrayList;

import org.nuxeo.data.gen.pdf.PDFTemplateGenerator;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;

public class ITextIDTemplateCreator implements PDFTemplateGenerator {

	public static String mkTag(String value, int size) {
		String tag = "#" + value;
		return tag + "-".repeat(size - 1 - tag.length()) + "#";
	}

	public static final String[] _keys = new String[] {
			mkTag("NAME", 41), 
			mkTag("STREET", 20), 
			mkTag("CITY", 20),
			mkTag("STATE", 20), 
			mkTag("DATE", 20), 
			mkTag("ACCOUNTID", 22), };

	public static final String ACCOUNT_LABEL = "Primary Account Number: ";

	protected ImageData img;

	protected ArrayList<String> keys = new ArrayList<String>();

	
	@Override
	public String[] getKeys() {
		return keys.toArray(new String[keys.size()]);
	}


	protected String key(int idx) {
		return getKeys()[idx];
	}

	@Override
	public void init(InputStream in) throws Exception {

		if (in != null) {
			img = ImageDataFactory.create(in.readAllBytes());
		}
		
		for (String k : _keys) {
			keys.add(k);
		}

		keys.add(mkTag("TITLE", 25));
		keys.add(mkTag("DOB", 20));
		keys.add(mkTag("IDNUM", 20));
		keys.add(mkTag("EXP", 20));
	}

	@Override
	public void generate(OutputStream out) throws Exception {

		PdfDocument pdfDocument = new HackedPDFDocument(new PdfWriter(out));
		
		PageSize psize = PageSize.A8.rotate();
		psize.decreaseHeight(28);		
		pdfDocument.setDefaultPageSize(psize);		
		
		Document document = new Document(pdfDocument);
		document.setMargins(0, 0, 0, 0);
		
		if (img != null) {
			document.add(new Image(img).scale(0.24f, 0.24f).setFixedPosition(0.1f, 0.3f));
		}

		Color titleColor = ColorConstants.BLUE;
		document.add(new Paragraph().setTextAlignment(TextAlignment.CENTER).setPaddingTop(8f)
				.add(new Text(key(6))).setFontSize(12).setBold().setFontColor(titleColor)).setFixedPosition(0.3f, 0.3f, UnitValue.POINT);
		
		document.add(new Paragraph().setTextAlignment(TextAlignment.RIGHT).setPaddingTop(0f).setPaddingRight(12f).setPaddingBottom(0f)
				.add(new Text(key(0) )).setFontSize(8).add(new Text("\n DOB " + key(7) ).setFontSize(5))).setFixedPosition(0.3f, 0.3f, UnitValue.POINT);

		document.add(new Paragraph().setTextAlignment(TextAlignment.CENTER).setPaddingTop(0f).setPaddingRight(0f).setPaddingBottom(0f)
				.add(new Text(key(1))).add(new Text("\n" +key(2) + "\n" +key(3))).setFontSize(6)).setFixedPosition(0.3f, 0.3f, UnitValue.POINT);
		
		document.add(new Paragraph().setTextAlignment(TextAlignment.RIGHT).setPaddingTop(0f).setPaddingRight(14f).setPaddingBottom(0f)
				.add(new Text("expires " + key(9) )).setFontSize(5)).setFixedPosition(0.3f, 0.3f, UnitValue.POINT);
		
		document.close();
	}


}
