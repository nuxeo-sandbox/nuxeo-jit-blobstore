package org.nuxeo.data.gen.docx;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.nuxeo.data.gen.BaseBankGenerator;
import org.nuxeo.data.gen.BaseBankTemplate;
import org.nuxeo.data.gen.pdf.PDFFileGenerator;
import org.nuxeo.data.gen.pdf.itext.filter.PDFOutputFilter;

import com.itextpdf.kernel.pdf.PdfDocument;

public class DocXLetterGenerator extends BaseBankGenerator implements PDFFileGenerator {

	protected File docXTemplate;
	
	protected String xmlTemplate;
	
	protected static final String DOC_ENTRY="word/document.xml";
	
	protected String[] keys;
		
	public void defaultInit() throws Exception {	
		InputStream docXTemplate = DocXLetterGenerator.class.getResourceAsStream("/new_account.docx");
		init(docXTemplate, BaseBankTemplate._keys);		
	}
	
	@Override
	public void init(InputStream docXTemplate, String[] keys) throws Exception {

		this.keys=keys;
		File tmp = File.createTempFile("tmp", "zip");
		Files.copy(docXTemplate, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
		this.docXTemplate = tmp;			
		readXMLTemplate();		
		this.computeDigest=true;
	}

	protected void readXMLTemplate() throws Exception {
		ZipFile zipFile = new ZipFile(docXTemplate);
		InputStream is=null;
		try {
			
			is = zipFile.getInputStream(new ZipEntry(DOC_ENTRY));						
			xmlTemplate = new String(IOUtils.toByteArray(is));
		} finally {
			is.close();
			zipFile.close();	
		}
	}
		
	protected byte[] generateContent(String[] tokens) {
        String xml = new String(xmlTemplate);

        for (int i = 0; i < keys.length; i++) {
        	xml = xml.replace(keys[i], tokens[i]);
        }
        return xml.getBytes();
	}

	@Override
	public String getType() {
		return "NewAccountLetter";
	}

	@Override
	public void setFilter(PDFOutputFilter filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void postProcessDoc(PdfDocument doc) throws Exception {
		// NOP
	}

	@Override
	protected void doGenerate(OutputStream buffer, String[] tokens) throws Exception {

		ZipFile zipFile = new ZipFile(docXTemplate);
		final ZipOutputStream zos = new ZipOutputStream(buffer);
		for(Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
		    ZipEntry entryIn = (ZipEntry) e.nextElement();
		    if (!entryIn.getName().equalsIgnoreCase(DOC_ENTRY)) {
		        zos.putNextEntry(entryIn);
		        InputStream is = zipFile.getInputStream(entryIn);
		        byte[] buf = new byte[1024];
		        int len;
		        while((len = is.read(buf)) > 0) {            
		            zos.write(buf, 0, len);
		        }
		    }
		    else{
		        zos.putNextEntry(new ZipEntry(DOC_ENTRY));
		        byte[] buf = generateContent(tokens);		        
		        zos.write(buf, 0, buf.length);
		    }
		    zos.closeEntry();
		}
		zos.close();
		zipFile.close();		
	}

	@Override
	protected String getDefaultExtension() {
		return ".docx";
	}

}
