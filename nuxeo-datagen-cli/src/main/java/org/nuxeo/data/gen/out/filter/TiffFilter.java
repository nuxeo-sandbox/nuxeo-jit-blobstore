package org.nuxeo.data.gen.out.filter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.itextpdf.io.source.ByteArrayOutputStream;

public class TiffFilter implements WriteFilter {

	public static final String NAME = "tiff";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public InputStream wrap(byte[] pdf) throws Exception {

		PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf));
		PDFRenderer renderer = new PDFRenderer(doc);

		ImageWriter tiffWriter = ImageIO.getImageWritersByFormatName("TIFF").next();
		BufferedImage bim = renderer.renderImageWithDPI(0, 150, ImageType.RGB);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ImageOutputStream outStream = ImageIO.createImageOutputStream(out);
		tiffWriter.setOutput(outStream);
		
		ImageWriteParam params = tiffWriter.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionType("Deflate");
        
        //tiffWriter.write(bim);

        // writing it this way produce a much smaller file ...
        tiffWriter.prepareWriteSequence(null);
        IIOMetadata metadata = tiffWriter.getDefaultImageMetadata(new ImageTypeSpecifier(bim), params);
        tiffWriter.writeToSequence(new IIOImage(bim, null, metadata), params);
        tiffWriter.endWriteSequence();

        doc.close();
        tiffWriter.dispose();
        outStream.flush();
        outStream.close();
        out.flush();
         
		InputStream result = new ByteArrayInputStream(out.toByteArray());				
		return result;
	}
	
	@Override
	public String getExtension() {
		return "tif";
	}

}
