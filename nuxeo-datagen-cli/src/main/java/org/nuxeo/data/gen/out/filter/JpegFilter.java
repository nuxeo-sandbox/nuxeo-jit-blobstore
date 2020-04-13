package org.nuxeo.data.gen.out.filter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.itextpdf.io.source.ByteArrayOutputStream;

public class JpegFilter extends AbstractFilter implements WriteFilter {

	public static final String NAME = "jpeg";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public InputStream wrap(byte[] pdf) throws Exception {

		PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf));
		PDFRenderer renderer = new PDFRenderer(doc);

		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(0.5f);		

		BufferedImage bim = renderer.renderImageWithDPI(0, getDPI(), ImageType.RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MemoryCacheImageOutputStream outStream = new MemoryCacheImageOutputStream(out);
		jpgWriter.setOutput(outStream);
		jpgWriter.write(bim);
		
		doc.close();
		jpgWriter.dispose();
        outStream.flush();
        outStream.close();
        out.flush();

        InputStream result = new ByteArrayInputStream(out.toByteArray());
        
		return result;
	}

	@Override
	public String getExtension() {
		return "jpeg";
	}

}
