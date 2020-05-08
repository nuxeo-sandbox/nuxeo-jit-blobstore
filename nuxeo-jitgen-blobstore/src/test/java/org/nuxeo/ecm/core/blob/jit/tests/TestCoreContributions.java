package org.nuxeo.ecm.core.blob.jit.tests;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/schemamanager-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/versioning-contrib.xml")
public class TestCoreContributions {

	
	@Inject
	protected CoreSession session;

	@Test
	public void canCreateDocuments() throws Exception {
						
		DocumentModel doc = session.createDocumentModel("File");	
		doc.setPathInfo("/", "file");
		doc.setPropertyValue("dc:title", "Yo");						
		doc = session.createDocument(doc);		
		doc = session.getDocument(doc.getRef());
		
	}

	
}
