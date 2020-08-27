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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
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
import org.nuxeo.importer.DummyIdPathCache;
import org.nuxeo.importer.BasePath2IdCache;
import org.nuxeo.importer.stream.jit.automation.ParentRefHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestParentRefHack {

	@Inject
	protected CoreSession session;
	
	@Test
	public void testPath() {
		
		String rootPath="/";
		String parentPath = "/ohio/1EF7F-0E96E-6A875F3";
		
		Path p = new Path(rootPath);
		p = p.append(parentPath);
		assertFalse(p.toString().contains("//"));	
	}

	@Test
	public void testCreateWithpreSetParentIdRef() throws Exception {
						
		DocumentModel root = session.createDocumentModel("Folder");	
		root.setPathInfo("/", "MyRoot");
		root.setPropertyValue("dc:title", "Yo");						
		root = session.createDocument(root);
		
		
		ParentRefHelper.init();
		assertNotNull(ParentRefHelper.getInstance());
		
		DocumentModel doc = session.createDocumentModel("File");	
		doc.setPathInfo("/MyRoot", "myFile");

		ParentRefHelper.getInstance().setParentRef(session, doc, "/MyRoot");
		
		DocumentRef pRef = doc.getParentRef();
		
		assertNotNull(pRef);
		assertTrue(pRef instanceof IdRef);
		assertEquals(pRef,  root.getRef());
		
		doc.setPropertyValue("dc:title", "Yo");
						
		doc = session.createDocument(doc);
		
		assertEquals(root.getRef(), ParentRefHelper.getInstance().getFromCache("/MyRoot"));		
		
	}
	
	@Test
	public void testCacheHits() {
		
		int nbMonths=6;
		int nbTotalAccounts= 1000000;
		SequenceGenerator sg = new SequenceGenerator(nbMonths);
		
		BasePath2IdCache cache = new DummyIdPathCache(100);
		
		for (int i = 0; i < nbTotalAccounts*nbMonths; i++) {
			
			SequenceGenerator.Entry e =sg.next();	

			if (i%50000==0) {
				System.out.println(i);
			}
			
			String accountId = e.getAccountID();
			String customerId = accountId.substring(0,19);
			String accountKey = accountId.substring(20);
			String state = e.getMetaData()[3];
			
			String path = "/" + state + "/" + customerId + "/" + accountKey;
			
			cache.resolveWithCache(session, path);			
			
		}
		
		System.out.println(cache.getHitRatio());
				
	}

	
	
}
