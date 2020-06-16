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
package org.nuxeo.ecm.core.blob.jit.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.runtime.api.Framework;

public class BlobTextExtractor {

	private static final Log log = LogFactory.getLog(BlobTextExtractor.class);

	protected static BlobTextExtractor instance = new BlobTextExtractor();

	protected static final String PDF_TEXT_EXTRACTOR_PROP = "org.nuxeo.bencmark.pdf.text.extractor";

	protected final String extractorName;

	protected static final String STMT_TEMPLATE_TXT = "Primary Account Number:\n" + "Date: Label: Debit: Credit:\n"
			+ "Beginning Balance\n";

	public static BlobTextExtractor instance() {
		return instance;
	}

	public BlobTextExtractor() {
		extractorName = Framework.getProperty(PDF_TEXT_EXTRACTOR_PROP, PDFBoxFTExtractor2.class.getSimpleName());
	}

	protected PDFFulltextExtractor getExtractor() {
		if (iTextFTExtractor.class.getSimpleName().equalsIgnoreCase(extractorName)) {
			return new iTextFTExtractor();
		} else if (PDFBoxFTExtractor2.class.getSimpleName().equalsIgnoreCase(extractorName)) {
			return new PDFBoxFTExtractor2();
		} else if ("none".equalsIgnoreCase(extractorName)) {
			return null;
		}
		return new PDFBoxFTExtractor();
	}

	protected List<String> cleanTxt(String[] data) {		
		List<String> result = new ArrayList<String>();
		for (String s : data) {
			s = s.trim();
			if (s.charAt(0)=='$') {
				continue;
			}
			result.add(s);			
		}
		return result;
	}
	protected String getTextFromBlobProvider(String providerId, String key) {
		try {
			InMemoryBlobGenerator imbg = Framework.getService(InMemoryBlobGenerator.class);
			String[] meta = ((StatementsBlobGenerator) imbg).getMetaDataForBlobKey(key);
			return STMT_TEMPLATE_TXT + String.join(" ", cleanTxt(meta));
		} catch (Exception e) {
			log.error("Unable to extract Full Text for BlobKey " + key, e);
			return STMT_TEMPLATE_TXT;
		}
	}

	public String getTextFromBlob(Blob blob) throws IOException {		
		if (blob==null) {
			return "";
		}		
		ManagedBlob mblob = (ManagedBlob) blob;		
		String blobKey = mblob.getKey();
		String keys[] = blobKey.split(":");

		if ("jit".equalsIgnoreCase(keys[0])) {
			return getTextFromBlobProvider(keys[0], keys[1]);
		} else {
			String mtype = mblob.getMimeType();
			if ("application/pdf".equalsIgnoreCase(mtype)) {
				PDFFulltextExtractor extractor = getExtractor();
				if (extractor != null) {
					try {
						return extractor.getText(blob.getStream());
					} catch (Exception e) {
						throw new IOException(e);
					}
				} else {
					return "";
				}	
			} else {
				log.warn("Statement has a blob of type " + mtype + ": skipping Fulltext");
				return "";
			}
		}		
	}
}
