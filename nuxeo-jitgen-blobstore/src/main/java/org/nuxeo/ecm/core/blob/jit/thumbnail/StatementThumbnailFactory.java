package org.nuxeo.ecm.core.blob.jit.thumbnail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
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
		List<PDPage> pdPages = doc.getDocumentCatalog().getAllPages();											
	    BufferedImage bim = pdPages.get(0).convertToImage(BufferedImage.TYPE_BYTE_INDEXED, 36);
	    ByteArrayOutputStream out = new ByteArrayOutputStream();				    
	    ImageIOUtil.writeImage(bim, "JPEG", out, 36, 0.5f);
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
