package org.nuxeo.ecm.core.blob.jit.thumbnail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

public class StatementThumbnailFactory implements ThumbnailFactory {

    private static final Log log = LogFactory.getLog(StatementThumbnailFactory.class);

    public static final String THUMBNAIL_FACET = "Thumbnail";

    public static final String THUMBNAIL_MIME_TYPE = "image/jpeg";

    public static final String THUMBNAIL_PROPERTY_NAME = "thumb:thumbnail";

    public static final String ANY_TO_THUMBNAIL_CONVERTER_NAME = "anyToThumbnail";

    public static final String PDF_AND_IMAGE_TO_THUMBNAIL_CONVERTER_NAME = "pdfAndImageToThumbnail";

    public static final String ANY_TO_PDF_TO_THUMBNAIL_CONVERTER_NAME = "anyToPdfToThumbnail";

    public static final String THUMBNAIL_SIZE_PARAMETER_NAME = "size";

    public static final String THUMBNAIL_DEFAULT_SIZE = "1000x1000";

    
	@Override
	public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
		return null;
	}

	@Override
	public Blob getThumbnail(DocumentModel doc, CoreSession session) {
		 ConversionService conversionService;
	        Blob thumbnailBlob = null;
	        try {
	            conversionService = Framework.getService(ConversionService.class);
	            BlobHolder bh = doc.getAdapter(BlobHolder.class);
	            if (bh != null) {
	                Map<String, Serializable> params = new HashMap<>();
	                // Thumbnail converter
	                params.put(THUMBNAIL_SIZE_PARAMETER_NAME, THUMBNAIL_DEFAULT_SIZE);
	                bh = conversionService.convert(ANY_TO_THUMBNAIL_CONVERTER_NAME, bh, params);
	                if (bh != null) {
	                    thumbnailBlob = bh.getBlob();
	                }
	            }
	        } catch (NuxeoException e) {
	            log.warn("Cannot generate on the fly thumbnail", e);
	        }
	        return thumbnailBlob;
	}

}
