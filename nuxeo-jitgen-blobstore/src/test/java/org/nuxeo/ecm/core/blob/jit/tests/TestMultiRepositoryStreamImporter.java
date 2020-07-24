package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.importer.stream.automation.DocumentConsumers;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.jit.automation.CustomerFolderProducers;
import org.nuxeo.importer.stream.jit.automation.CustomerProducers;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-blobdispatcher-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/import-node-config.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-storage-repo-mem-contrib.xml")
@PartialDeploy(bundle = "studio.extensions.nuxeo-benchmark-10b-2020", extensions = {
		TargetExtensions.ContentModel.class, TargetExtensions.ContentTemplate.class })
public class TestMultiRepositoryStreamImporter {

	@Inject
	protected AutomationService automationService;

	@Inject
	protected WorkManager workManager;

	@Inject
	protected BulkService bulk;

	@Inject
	protected CoreSession session;
	
	@Inject
	protected RepositoryManager rm;

	@Inject
	ElasticSearchService ess;

	@Inject
	ElasticSearchIndexing esi;

	@Inject
	ElasticSearchAdmin esa;

	public void waitForCompletion() throws Exception {
		bulk.await(Duration.ofSeconds(20));
		workManager.awaitCompletion(20, TimeUnit.SECONDS);
		esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
		esa.refresh();
	}

	
	
	@Test
	public void testMultiRepository() throws Exception {

		Collection<Repository> repos = rm.getRepositories();

		List<String> repoNames = new ArrayList<>();

		for (Repository repo : repos) {
			repoNames.add(repo.getName());
		}

		Assert.assertTrue(repoNames.contains("us-east"));
		Assert.assertTrue(repoNames.contains("us-west"));
		
		Object repo1 = Framework.getService(RepositoryService.class)
         .getRepository("us-east");
		assertNotNull(repo1);
		
		boolean ft = Framework.getService(RepositoryService.class)
        .getRepository("us-east")
		.getFulltextConfiguration().fulltextSearchDisabled;

	}

	protected String initSession(CoreSession session, String blobTextContent) {

		// lazy way to create a Blob
		DocumentModel file = session.createDocumentModel("/", "someFilet", "File");
		Blob idblob = new StringBlob(blobTextContent);
		idblob.setFilename("whatever.jpg");
		file.setPropertyValue("file:content", (Serializable) idblob);
		file = session.createDocument(file);
		idblob = (Blob) file.getPropertyValue("file:content");
		String blobDigest = idblob.getDigest();
		System.out.println(blobDigest);

		return blobDigest;
	}

	@Test
	public void testCreate() {
		
		DocumentModel c = session.createDocumentModel("/", "bibi", "Customer");
		c = session.createDocument(c);
			
		DocumentModel id = session.createDocumentModel("/bibi", "idcard", "IDCard");
		
		Blob idblob = new StringBlob("whater");
		idblob.setFilename("whatever.jpg");
		id.setPropertyValue("file:content", (Serializable) idblob);
		id = session.createDocument(id);	
		
	}
	
	
	
	@Test
	public void canImportCustomers() throws Exception {

		CoreSession eastSession = CoreInstance.getCoreSession(USStateHelper.EAST);
		CoreSession westSession = CoreInstance.getCoreSession(USStateHelper.WEST);

		String blobTextContent = "I am a fake ID card!";
		String blobDigest = initSession(eastSession, blobTextContent);
		blobDigest = initSession(westSession, blobTextContent);

		DocumentModel rootEst = eastSession.createDocumentModel("/", "root", "Folder");
		rootEst = eastSession.createDocument(rootEst);

		DocumentModel rootWest = westSession.createDocumentModel("/", "root", "Folder");
		rootWest = westSession.createDocument(rootWest);

		TransactionHelper.commitOrRollbackTransaction();
		TransactionHelper.startTransaction();

		String logName = "import/hierarchy";

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			// *************************
			// create Folder messages
			params.put("logConfig", "chronicle");
			params.put("split", true);
			params.put("logName", logName);
			automationService.run(ctx, CustomerFolderProducers.ID, params);
		}

		int stateCounts = 0;
		try (OperationContext ctx = new OperationContext(eastSession)) {
			Map<String, Serializable> params = new HashMap<>();
			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("rootFolder", rootEst.getPathAsString());
			params.put("logConfig", "chronicle");
			params.put("logName", logName + "-" + USStateHelper.EAST);
			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = eastSession
					.query("select * from Domain where ecm:path STARTSWITH '/root' order by ecm:path");
			System.out.println("Docs in the East repository");
			dump(docs);
			stateCounts += docs.size();
			assertTrue(docs.size()>0);
		}
		try (OperationContext ctx = new OperationContext(westSession)) {
			Map<String, Serializable> params = new HashMap<>();
			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("rootFolder", rootWest.getPathAsString());
			params.put("logConfig", "chronicle");
			params.put("logName", logName + "-" + USStateHelper.WEST);

			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = westSession
					.query("select * from Domain where ecm:path STARTSWITH '/root' order by ecm:path");
			System.out.println("Docs in the WEST repository");
			dump(docs);
			stateCounts += docs.size();
			assertTrue(docs.size()>0);
		}
		assertEquals(USStateHelper.STATES.length, stateCounts);

		logName = "import/customers";

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			// *************************
			// create Customer messages
			params.put("logConfig", "chronicle");
			params.put("split", true);
			params.put("logName", logName);

			InputStream csv = StatementsBlobGenerator.class.getResourceAsStream("/id-cards.csv");
			String csvContent = new String(IOUtils.toByteArray(csv));
			csvContent = csvContent.replace("<DIGEST>", blobDigest);
			Blob blob = new StringBlob(csvContent);
			ctx.setInput(blob);

			automationService.run(ctx, CustomerProducers.ID, params);
		}

		int customerCount = 0;
		try (OperationContext ctx = new OperationContext(eastSession)) {
			Map<String, Serializable> params = new HashMap<>();
			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("rootFolder", rootEst.getPathAsString());
			params.put("logConfig", "chronicle");
			params.put("logName", logName + "-" + USStateHelper.EAST);
			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = eastSession.query(
					"select * from Document  where ecm:primaryType IN ('Customer', 'IDCard') and ecm:path STARTSWITH '/root' order by ecm:path");
			System.out.println("Docs in the East repository");
			dump(docs);
			customerCount += docs.size();
		}
		assertTrue(customerCount > 0);
		try (OperationContext ctx = new OperationContext(westSession)) {
			Map<String, Serializable> params = new HashMap<>();
			// *************************
			// consume Folder messages
			params = new HashMap<>();
			params.put("rootFolder", rootWest.getPathAsString());
			params.put("logConfig", "chronicle");
			params.put("logName", logName + "-" + USStateHelper.WEST);

			automationService.run(ctx, DocumentConsumers.ID, params);

			DocumentModelList docs = westSession.query(
					"select * from Document  where  ecm:primaryType IN ('Customer', 'IDCard') and ecm:path STARTSWITH '/root' order by ecm:path");
			System.out.println("Docs in the WEST repository");
			dump(docs);
			customerCount += docs.size();

		}
		
		assertEquals(200, customerCount);

	}

	protected void dump(DocumentModelList docs) {
		for (DocumentModel doc : docs) {
			System.out.println(doc.getType() + ": " + doc.getPathAsString() + " -- " + doc.getTitle());
		}
	}

}
