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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.data.gen.BaseBankTemplate;
import org.nuxeo.data.gen.pdf.PDFTemplateGenerator;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;

public class ITextNXBankTemplateCreator extends BaseBankTemplate implements PDFTemplateGenerator {

	public static final String ACCOUNT_LABEL = "Primary Account Number: ";

	protected class Operation {

		protected String label;

		protected Date date;

		protected double value;

		protected String strValue;
	}

	protected List<Operation> operations;
	protected ImageData img;

	protected void initOperations() {

		for (int i = 1; i < 15; i++) {
			Operation op = new Operation();

			op.label = "Operation " + i;

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, i);
			op.date = cal.getTime();

			op.value = Math.random() * 10000 - 5000;
			operations.add(op);
		}
	}

	protected String key(int idx) {
		return getKeys()[idx];
	}

	@Override
	public void init(InputStream in) throws Exception {

		operations = new ArrayList<ITextNXBankTemplateCreator.Operation>();
		initOperations();

		if (in != null) {
			img = ImageDataFactory.create(IOUtils.toByteArray(in));
		}
	}

	@Override
	public void generate(OutputStream out) throws Exception {

		PdfDocument pdfDocument = new HackedPDFDocument(new PdfWriter(out));

		pdfDocument.setDefaultPageSize(PageSize.LETTER);

		Document document = new Document(pdfDocument);

		if (img != null) {
			document.add(new Paragraph().add(new Image(img).scale(0.2f, 0.2f)));
		}

		document.add(new Paragraph().setTextAlignment(TextAlignment.LEFT).setMultipliedLeading(1)
				.add(new Text("\n" + key(0))).setFontSize(14).setBold().add(new Text("\n" + key(1))).setFontSize(14)
				.setBold().add(new Text("\n" + key(2))).setFontSize(14).setBold().add(new Text("\n" + key(3)))
				.setFontSize(14).setBold());
		document.add(new Paragraph().setTextAlignment(TextAlignment.RIGHT).setMultipliedLeading(1)
				.add(new Text(ACCOUNT_LABEL + key(5) + StringUtils.repeat(" ", 5))).setFontSize(14).setBold().add("\n" + key(4)));
		
		LineSeparator sep = new LineSeparator(new SolidLine());
		document.add(sep);

		document.add(buildOperationsList(operations));
		document.close();
	}

	protected Table buildOperationsList(List<Operation> operations) {

		Table table = new Table(
				new UnitValue[] { new UnitValue(UnitValue.PERCENT, 20.0f), new UnitValue(UnitValue.PERCENT, 40.0f),
						new UnitValue(UnitValue.PERCENT, 20.0f), new UnitValue(UnitValue.PERCENT, 20.0f) })
								.setWidth(UnitValue.createPercentValue(100)).setMarginTop(10).setMarginBottom(10);

		table.addHeaderCell(createCell("Date:").setTextAlignment(TextAlignment.CENTER).setBold());
		table.addHeaderCell(createCell("Label:").setTextAlignment(TextAlignment.CENTER).setBold());
		table.addHeaderCell(createCell("Debit:").setTextAlignment(TextAlignment.CENTER).setBold());
		table.addHeaderCell(createCell("Credit:").setTextAlignment(TextAlignment.CENTER).setBold());

		printOperation(table);

		return table;
	}

	protected void printOperation(Table table) {

		double total = 0;
		for (Operation op : operations) {
			table.addCell(createCell(new SimpleDateFormat("MMM dd").format(op.date)))
					.setTextAlignment(TextAlignment.LEFT);
			table.addCell(createCell(op.label).setTextAlignment(TextAlignment.CENTER));

			String formattedValue = NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(Math.abs(op.value));
			if (op.value > 0) {
				table.addCell(createCell(""));
				table.addCell(createCell(formattedValue)).setTextAlignment(TextAlignment.RIGHT);
			} else {
				table.addCell(createCell(formattedValue)).setTextAlignment(TextAlignment.RIGHT);
				table.addCell(createCell(""));
			}
			total += op.value;
		}
		String formattedValue = NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(Math.abs(total));
		table.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
		table.addCell(createCell(formattedValue)).setTextAlignment(TextAlignment.RIGHT);

	}

	/**
	 * Creates a cell with specific properties set.
	 *
	 * @param text the text that will be in the cell
	 * @return the cell
	 */

	public Cell createCell(String text) {
		return new Cell().setPadding(0.8f).add(new Paragraph(text).setMultipliedLeading(1));
	}

	/**
	 * Creates a cell with specific properties set.
	 *
	 * @param text the text that will be in the cell
	 * @param font the font
	 * @return the cell
	 */
	public Cell createCell(String text, PdfFont font) {
		return new Cell().setPadding(0.8f).add(new Paragraph(text).setFont(font).setMultipliedLeading(1));
	}

}
