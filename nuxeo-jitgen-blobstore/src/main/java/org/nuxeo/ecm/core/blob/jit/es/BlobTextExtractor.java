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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.runtime.api.Framework;

public class BlobTextExtractor {
	
	protected static BlobTextExtractor instance = new BlobTextExtractor(); 
	
	protected static final String STMT_TEMPLATE_TXT= "Primary Account Number:\n" + 
			"Date: Label: Debit: Credit:\n" + 
			"Beginning Balance\n"; 
			
	
	public static BlobTextExtractor instance() {		
		return instance;
	}
	
	protected static final String PDF_TEXT_EXTRACTOR_PROP="org.nuxeo.bencmark.pdf.text.extractor";
	
	protected final String extractorName;
	
	public BlobTextExtractor() {
		extractorName = Framework.getProperty(PDF_TEXT_EXTRACTOR_PROP, null);
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
	
	protected String getTextFromBlobProvider(Blob blob) {		
		ManagedBlob mblob = (ManagedBlob) blob;		
		String key = mblob.getKey();
		key = key.split(":")[1];		
		InMemoryBlobGenerator imbg = Framework.getService(InMemoryBlobGenerator.class);
		String[] meta = ((StatementsBlobGenerator)imbg).getMetaDataForBlobKey(key);		
		return STMT_TEMPLATE_TXT + String.join(" ", meta);
	}
	
	public String getTextFromPDF(Blob blob) throws IOException {
		
		if (extractorName!=null) {
			PDFFulltextExtractor extractor = getExtractor();
			if (extractor!=null) {
				try {
					return extractor.getText(blob.getStream());
				} catch (Exception e) {
					throw new IOException(e);
				}
			} else {
				return "";
			}			
		} else {
			return getTextFromBlobProvider(blob);
		}		
	}

	
}
