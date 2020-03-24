package org.nuxeo.ecm.core.blob.jit.gen.pdf.itext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.blob.jit.gen.pdf.PDFTemplateGenerator;

import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;

public class ITextNXBankTemplateCreator2 extends ITextNXBankTemplateCreator implements PDFTemplateGenerator {

	protected ArrayList<String> keys = new ArrayList<String>();
	
	@Override
	protected void initOperations() {

		for (String k : _keys) {
			keys.add(k);
		}
		
		keys.add(mkTag("MON", 5));
		
		Operation obb = new Operation();
		obb.label="Beginning Balance";
		Calendar cal = new GregorianCalendar(2020, 0, 1);
		obb.date = cal.getTime();

		obb.strValue=mkTag("Balance",12);		
		keys.add(obb.strValue);
		operations.add(obb);

		cal.add(Calendar.DAY_OF_MONTH, 3);
		
		for (int i = 1; i < 15; i++) {
			Operation op = new Operation();

			
			op.label = mkTag("OP-" + String.format("%02d", i), 30) ;
			keys.add(op.label);			
						
			cal.add(Calendar.DAY_OF_MONTH, 1);
			op.date = cal.getTime();

			op.strValue=mkTag("OPVal" + String.format("%02d", i) ,12);
			keys.add(op.strValue);			
			
			operations.add(op);
		}
		
		keys.add(mkTag("Total",12));	
		
	}

	@Override
	public String[] getKeys() {
		return keys.toArray(new String[keys.size()]);
	}

	@Override
	protected void printOperation(Table table) {

		for (Operation op : operations) {
			table.addCell(createCell("#MON#" +new SimpleDateFormat(" dd").format(op.date)))
					.setTextAlignment(TextAlignment.LEFT);
			table.addCell(createCell(op.label).setTextAlignment(TextAlignment.CENTER));

			if (op.label.startsWith("#OP")) {
				table.addCell(createCell(op.strValue)).setTextAlignment(TextAlignment.CENTER);
				table.addCell(createCell("").setBackgroundColor(WebColors.getRGBColor("#DDDDDD")));				
			} else {
				table.addCell(createCell("").setBackgroundColor(WebColors.getRGBColor("#DDDDDD")));
				table.addCell(createCell(op.strValue)).setTextAlignment(TextAlignment.CENTER);
			} 
		}
		table.addCell(new Cell(1, 4).setBorder(Border.NO_BORDER));
		table.addCell(new Cell(1, 3).setBorder(Border.NO_BORDER));
		table.addCell(createCell(mkTag("Total", 12)+ "     ")).setTextAlignment(TextAlignment.CENTER);		
	}

}
