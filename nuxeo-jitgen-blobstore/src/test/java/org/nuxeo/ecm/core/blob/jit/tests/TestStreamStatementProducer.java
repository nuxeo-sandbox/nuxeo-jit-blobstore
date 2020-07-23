package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.data.gen.meta.SequenceGenerator;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.jit.gen.StatementsBlobGenerator;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.jit.StatementFolderMessageProducer;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.jit.automation.CustomerFolderProducers;
import org.nuxeo.importer.stream.jit.automation.CustomerProducers;
import org.nuxeo.importer.stream.jit.automation.StatementFolderProducers;
import org.nuxeo.importer.stream.jit.automation.StatementProducers;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/elasticsearch-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
public class TestStreamStatementProducer {

	@Inject
	protected CoreSession session;

	@Inject
	protected AutomationService automationService;

	@Test
	public void canCreateTimeHierarchyMessages() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");

			automationService.run(ctx, StatementFolderProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;
			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					assertEquals("Folder", docMessage.getType());
					assertTrue(((String) docMessage.getProperties().get("dc:description"))
							.startsWith(StatementFolderMessageProducer.FOLDER_DESC_PREFIX));
					count++;
				}
			} while (record != null);

			assertEquals(48 + 4, count);
			tailer.commit();
			tailer.close();
		}
	}

	@Test
	public void canCreateStatesHierarchyMessages() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			params.put("logConfig", "chronicle");

			automationService.run(ctx, CustomerFolderProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;

			List<String> names = new ArrayList<String>();

			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					assertEquals("Folder", docMessage.getType());
					assertFalse(names.contains(docMessage.getName()));
					names.add(docMessage.getName());
					count++;
				}
			} while (record != null);

			assertEquals(USStateHelper.STATES.length, count);
			tailer.commit();
			tailer.close();
		}
	}

	@Test
	public void canCreateCustomerMessages() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			params.put("logConfig", "chronicle");
			params.put("bufferSize", "5");

			InputStream csv = StatementsBlobGenerator.class.getResourceAsStream("/id-cards.csv");
			Blob blob = new StringBlob(new String(IOUtils.toByteArray(csv)));

			ctx.setInput(blob);
			automationService.run(ctx, CustomerProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;

			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					//assertEquals("CustomerDocument", docMessage.getType());
					//System.out.println(docMessage.getType() +":"+ docMessage.getParentPath() + "/" + docMessage.getName());
					count++;
				}
			} while (record != null);

			assertEquals(200, count);
			tailer.commit();
			tailer.close();
		}
	}

	
	@Test
	public void canCreateCustomerMessagesMultiRepo() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			String logName = "import/consumers";
			
			params.put("logConfig", "chronicle");
			params.put("bufferSize", "5");
			params.put("split", true);
			params.put("logName", logName);
			
			InputStream csv = StatementsBlobGenerator.class.getResourceAsStream("/id-cards.csv");
			Blob blob = new StringBlob(new String(IOUtils.toByteArray(csv)));

			ctx.setInput(blob);
			automationService.run(ctx, CustomerProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer1 = manager.createTailer("test", logName + "-" + USStateHelper.EAST);
			LogTailer<DocumentMessage> tailer2 = manager.createTailer("test", logName + "-" + USStateHelper.WEST);
			int count = 0;

			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer1.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					System.out.println("EAST: " + docMessage.getParentPath() + '/' + docMessage.getName());
					assertTrue(USStateHelper.isEastern(getState(docMessage)));
					count++;
				}
			} while (record != null);

			assertTrue(count>0);

			do {
				record = tailer2.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					System.out.println("EAST: " + docMessage.getParentPath() + '/' + docMessage.getName());
					assertFalse(USStateHelper.isEastern(getState(docMessage)));
					count++;
				}
			} while (record != null);

			
			assertEquals(200, count);
			tailer1.commit();
			tailer1.close();
			tailer2.commit();
			tailer2.close();

		}
	}

	protected static String getState(DocumentMessage docMessage) {
		Map<String, String> address = (Map<String, String>) docMessage.getProperties().get("customer:address");
		return address.get("state");
	}
	protected static String[] expectedAccountID = new String[] { "0E570-08A8E-53AE1E6-01", "0D377-0F93A-3A16029-01",
			"0D377-0F93A-3A16029-02", "0D377-0F93A-3A16029-03", "07180-05BE9-44A4265-01", "07180-05BE9-44A4265-02",
			"13A20-0E86C-69A4770-01", "19E08-06256-59DD19E-01", "05F4E-0100B-044C4D7-01", "05F4E-0100B-044C4D7-02" };

	@Test
	public void canCreateStatementsMessages() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			long nbDocs = 48 * 10;

			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 48);
			params.put("logConfig", "chronicle");
			params.put("nbThreads", 1);
			params.put("seed", SequenceGenerator.DEFAULT_ACCOUNT_SEED);

			automationService.run(ctx, StatementProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;
			int idx = 0;
			String lastAccounId = null;
			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					assertEquals("Statement", docMessage.getType());
					assertEquals("initialImport", docMessage.getProperties().get("dc:source"));

					String account = (String) docMessage.getProperties().get("account:number");
					String customer = (String) docMessage.getProperties().get("customer:number");

					if (count % 48 == 0) {
						lastAccounId = account;
						assertEquals(expectedAccountID[idx], lastAccounId);
						// System.out.println(lastAccounId);
						assertTrue(expectedAccountID[idx].contains(customer));
						idx++;
					} else {
						assertEquals(lastAccounId, account);
					}
					count++;
				}
			} while (record != null);

			assertEquals(nbDocs, count);
			tailer.commit();
			tailer.close();
		}

	}

	@Test
	public void canCreateStatementsMessagesMT() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			long nbThreads = 8;
			long nbMonths = 1;
			long nbDocs = nbMonths * 10 * nbThreads;

			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", nbMonths);
			params.put("logConfig", "chronicle");
			params.put("nbThreads", nbThreads);
			params.put("seed", SequenceGenerator.DEFAULT_ACCOUNT_SEED);

			automationService.run(ctx, StatementProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			List<String> generatedAccounts = new ArrayList<String>();

			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(10));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					String account = (String) docMessage.getProperties().get("account:number");
					generatedAccounts.add(account);
				}
			} while (record != null);

			assertEquals(nbDocs, generatedAccounts.size());
			tailer.commit();
			tailer.close();

			// System.out.println(generatedAccounts);

			Set<String> dups = findDups(generatedAccounts);
			// XXX there is a Schrodinger cat hidden in here !
			// System.out.println(dups);
			assertEquals(0, dups.size());

			// verify that we generated all the expected accounts
			List<String> thread1Accounts = new ArrayList<String>();

			for (String ac : expectedAccountID) {
				assertTrue("Unable to find entry " + ac, generatedAccounts.contains(ac));
				thread1Accounts.add(ac);
			}

			int nbOtherAccounts = 0;
			for (String ac : generatedAccounts) {
				if (!thread1Accounts.contains(ac)) {
					nbOtherAccounts++;
				}
			}
			assertEquals(nbDocs, expectedAccountID.length + nbOtherAccounts);
		}
	}

	@Test
	public void canCreateStatementsMessagesForOneMonth() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			long nbDocs = 1000;

			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 1);
			params.put("monthOffset", 48);
			params.put("logConfig", "chronicle");
			params.put("nbThreads", 1);
			params.put("seed", SequenceGenerator.DEFAULT_ACCOUNT_SEED);

			automationService.run(ctx, StatementProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;
			String lastAccountId = null;
			Date lastDate = null;
			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					assertEquals("Statement", docMessage.getType());
					assertEquals("initialImport", docMessage.getProperties().get("dc:source"));

					String account = (String) docMessage.getProperties().get("account:number");
					Date date = (Date) docMessage.getProperties().get("all:documentDate");

					String tag = (String) docMessage.getProperties().get("dc:publisher");
					assertNotNull(tag);
					
					if (lastDate != null) {
						assertTrue(lastDate.equals(date));
					}
					lastDate = date;

					if (lastAccountId != null) {
						assertFalse(lastAccountId.equals(account));
					}
					lastAccountId = account;

					count++;
				}
			} while (record != null);

			assertEquals(nbDocs, count);
			tailer.commit();
			tailer.close();
		}

	}

	
	@Test
	public void canCreateStatementsMessagesWithBatchTag() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			long nbDocs = 100;

			final String TAG = "yo";
			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 1);
			params.put("batchTag", TAG);
			params.put("logConfig", "chronicle");
			params.put("nbThreads", 1);
			
			automationService.run(ctx, StatementProducers.ID, params);

			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;
			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					assertEquals("Statement", docMessage.getType());
					assertEquals("initialImport", docMessage.getProperties().get("dc:source"));

					String tag = (String) docMessage.getProperties().get("dc:publisher");

					assertEquals(TAG, tag);
					count++;
				}
			} while (record != null);

			assertEquals(nbDocs, count);
			tailer.commit();
			tailer.close();
		}
	}

	protected Set<String> findDups(List<String> lst) {

		Set<String> dups = new HashSet<String>();
		Set<String> uniqueEntries = new HashSet<String>();

		for (String e : lst) {
			if (uniqueEntries.contains(e)) {
				dups.add(e);
			} else {
				uniqueEntries.add(e);
			}
		}

		return dups;
	}

	@Test
	public void canRegenerateTheSameAccounts() throws Exception {
		SequenceGenerator sGen = new SequenceGenerator(1);
		for (int i = 0; i < 10; i++) {
			String id = sGen.next().getAccountID();
			assertEquals(expectedAccountID[i], id);
		}

	}
	

	
	@Test
	public void canCreateStatementsMessagesWithStatesMT() throws Exception {

		try (OperationContext ctx = new OperationContext(session)) {
			Map<String, Serializable> params = new HashMap<>();

			long nbDocs = 1000000;

			params.put("nbDocuments", nbDocs);
			params.put("nbMonths", 60);
			params.put("withStates", true);
			params.put("logConfig", "chronicle");
			params.put("nbThreads", 16);
			
			long t0 = System.currentTimeMillis();
			automationService.run(ctx, StatementProducers.ID, params);
			long t1 = System.currentTimeMillis();
			
			LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");

			LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);

			int count = 0;
			LogRecord<DocumentMessage> record = null;
			do {
				record = tailer.read(Duration.ofSeconds(1));
				if (record != null) {
					DocumentMessage docMessage = record.message();
					assertEquals("Statement", docMessage.getType());
					assertEquals("initialImport", docMessage.getProperties().get("dc:source"));

					String tag = (String) docMessage.getProperties().get("dc:publisher");

					count++;
				}
			} while (record != null);

			double throughput = (1000.0*count) / (t1-t0);
			System.out.println("Doc Message Throughput = " + throughput);
			assertEquals(nbDocs, count);
			tailer.commit();
			tailer.close();
		}
	}

}
