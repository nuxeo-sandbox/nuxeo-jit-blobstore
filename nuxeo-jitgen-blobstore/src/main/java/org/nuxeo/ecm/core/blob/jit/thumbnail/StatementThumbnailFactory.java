package org.nuxeo.ecm.core.blob.jit.thumbnail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;

import com.itextpdf.io.source.ByteArrayOutputStream;

public class StatementThumbnailFactory implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(StatementThumbnailFactory.class);
    
    public static Blob computeThumb(byte[] pdf) throws Exception {
		PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf));
		
		PDFRenderer renderer = new PDFRenderer(doc);
				
		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(0.5f);		
		
		BufferedImage bim = renderer.renderImageWithDPI(0, 30, ImageType.RGB);
	    ByteArrayOutputStream out = new ByteArrayOutputStream();				    
	    	   
	    MemoryCacheImageOutputStream outStream = new MemoryCacheImageOutputStream(out);
	    jpgWriter.setOutput(outStream);
	    jpgWriter.write(bim);
	    
	    Blob blob = new ByteArrayBlob(out.toByteArray());
		doc.close();
		return blob;
	}
    
	@Override
	public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
		return null;
	}

	@Override
	public Blob getThumbnail(DocumentModel doc, CoreSession session) {
	        Blob thumbnailBlob = null;
	        try {
	            BlobHolder bh = doc.getAdapter(BlobHolder.class);	            
	            if (bh != null) {
	            	thumbnailBlob = computeThumb(bh.getBlob().getByteArray());
	            }
	        } catch (Exception e) {
	            log.warn("Cannot generate on the fly thumbnail", e);
	        }
	        return thumbnailBlob;
	}

}
