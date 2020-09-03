package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
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
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.jit.es.StatementESDocumentWriter;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.importer.stream.automation.DocumentConsumers;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.jit.automation.CustomerFolderProducers;
import org.nuxeo.importer.stream.jit.automation.CustomerProducers;
import org.nuxeo.importer.stream.jit.automation.DocumentConsumersEx;
import org.nuxeo.importer.stream.jit.automation.StatementFolderProducers;
import org.nuxeo.importer.stream.jit.automation.StatementProducers;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/scroll-contrib.xml")
@PartialDeploy(bundle = "studio.extensions.nuxeo-benchmark-10b-2020", extensions = {
		TargetExtensions.ContentModel.class })
public class TestStatementImportAndIndex {

	@Inject
	protected CoreSession session;

	@Inject
	protected AutomationService automationService;

	@Inject
	protected WorkManager workManager;

	@Inject
	protected BulkService bulk;

	@Inject
	protected ScrollService scrollService;

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
	public void checkScrollerDeployed() {

		GenericScrollRequest request = GenericScrollRequest.builder("DocConsumerScroller", "whatever").build();
		assertTrue(scrollService.exists(request));

	}

	@Test
	public void canImportStatementswithBAFIndexing() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			DocumentModel root = session.createDocumentModel("/", "root", "Folder");
			root = session.createDocument(root);
			TransactionHelper.commitOrRollbackTransaction();
			TransactionHelper.startTransaction();

			// *************************
			// create Statements messages
			long nbDocs = 1008;
			params = new HashMap<>();
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 48);
			params.put("storeInRoot", true);
			params.put("logConfig", "chronicle");
			automationService.run(ctx, StatementProducers.ID, params);

			// *************************
			// consume Statements messages
			params = new HashMap<>();
			params.put("nbThreads", 1);
			params.put("rootFolder", root.getPathAsString());
			params.put("logConfig", "chronicle");
			params.put("useScroller", true);	
			
			String json = (String) automationService.run(ctx, DocumentConsumersEx.ID, params);

			System.out.println(json);
			DocumentModelList docs = session.query("select * from Statement order by ecm:path");
			//dump(docs);
			assertEquals(nbDocs, docs.size());
			
			JsonNode node = new ObjectMapper().readTree(json);
			String cmdId = node.get("bulkIndexingCommandId").asText();
			
			
			bulk.await(cmdId, Duration.ofMinutes(1));
			
			BulkStatus status = bulk.getStatus(cmdId);

			System.out.println(status.toString());

			assertEquals(State.COMPLETED, status.getState());			
			assertEquals(0, status.getErrorCount());
			assertEquals(nbDocs, status.getProcessed());
			
		}
	}

	protected void dump(DocumentModelList docs) {
		for (DocumentModel doc : docs) {
			System.out.println(doc.getType() + ": " + doc.getPathAsString() + " -- " + doc.getTitle());
		}
	}

}
