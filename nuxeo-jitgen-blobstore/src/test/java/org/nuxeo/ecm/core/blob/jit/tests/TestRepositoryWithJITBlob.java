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
package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.jit.es.BlobTextExtractor;
import org.nuxeo.ecm.core.blob.jit.gen.DocInfo;
import org.nuxeo.ecm.core.blob.jit.gen.InMemoryBlobGenerator;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-blobdispatcher-contrib.xml")
public class TestRepositoryWithJITBlob {

	@Inject
	protected CoreSession session;
	
    @Inject
    protected BlobManager blobManager;

	@Inject
	protected InMemoryBlobGenerator imbg;

	@Test
	public void testCreateDocWithJITBlob() throws Exception {
						
		DocumentModel doc = session.createDocumentModel("File");	
		doc.setPathInfo("/", "file");
		doc.setPropertyValue("dc:title", "Yo");
					
		String key = imbg.computeKey(1L, 1L, 1);
		
		BlobInfo bi = imbg.computeBlobInfo("jit", key);
		Blob blob = new SimpleManagedBlob(bi);
		
		doc.setPropertyValue("file:content", (Serializable)blob); 
		doc.setPropertyValue("dc:source", "initialImport");
		
		doc = session.createDocument(doc);
		
		doc = session.getDocument(doc.getRef());
		
		blob = (Blob)doc.getPropertyValue("file:content");		
		
		System.out.println(blob.getFilename());
		
		InputStream pdf = blob.getStream();
		
		PDFTextStripper stripper = new PDFTextStripper();
		String txt = stripper.getText(PDDocument.load(pdf));
	    
		assertTrue(txt.contains("Account"));
		assertTrue(txt.contains("Balance"));
		
		
	}

	@Test
	public void testCreateDocStoredBlob() throws Exception {
						
		String blobContent = "Whatever";
		
		DocumentModel doc = session.createDocumentModel("File");	
		doc.setPathInfo("/", "file");
		doc.setPropertyValue("dc:title", "Yo");
		
		Blob blob = new StringBlob(blobContent);
		blob.setFilename("whatever.txt");				
		doc.setPropertyValue("file:content", (Serializable)blob); 
		//doc.setPropertyValue("dc:source", "initialImport");
		
		doc = session.createDocument(doc);
		
		doc = session.getDocument(doc.getRef());		
		blob = (Blob)doc.getPropertyValue("file:content");		
		
		assertEquals(blobContent, blob.getString());				
	}


	@Test
	public void testCreateDocWithJITBlobAndMeta() throws Exception {
						
		DocumentModel doc = session.createDocumentModel("File");	
		doc.setPathInfo("/", "file");
		doc.setPropertyValue("dc:title", "Yo");
					
		DocInfo di = imbg.computeDocInfo("jit", 1L, 1L, 1);
		
		doc.setPropertyValue("file:content", (Serializable)di.getBlob());		
		doc.setPropertyValue("dc:source", "initialImport");
		doc.setPropertyValue("dc:description", di.getMeta("ACCOUNTID"));		
		doc = session.createDocument(doc);
		
		doc = session.getDocument(doc.getRef());
		
		Blob blob = (Blob)doc.getPropertyValue("file:content");								
		InputStream pdf = blob.getStream();		
		PDFTextStripper stripper = new PDFTextStripper();
		
		String txt = stripper.getText(PDDocument.load(pdf));	    	
		assertTrue(txt.contains("Account"));
		assertTrue(txt.contains("Balance"));
		assertTrue(txt.contains(di.getMeta("ACCOUNTID")));
		
		assertEquals(di.getMeta("ACCOUNTID"), doc.getPropertyValue("dc:description"));
		
		// check FT extraction
		
		blob = (Blob)doc.getPropertyValue("file:content");
		String ft = BlobTextExtractor.instance().getTextFromBlob(blob);
		// should be the same except for the day numbers that are random...
		for (String keyword: txt.split("[\\n\\t ]")) {
			keyword = keyword.trim();
			if (!ft.contains(keyword.trim())) {
				keyword = keyword.replace(",", "");
				keyword = keyword.replace(".", "");
				keyword = keyword.replace("$", "");
				assertTrue(keyword.matches("^[0-9]+"));
			}
		}
	}

	
	
}
