package org.nuxeo.ecm.core.blob.jit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.blob.jit.rnd.AccountHelper;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.jit.StatementFolderMessageProducer;
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
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.importer.stream")

@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/JITBlobGen-service.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit:OSGI-INF/operations-contrib.xml")
@Deploy("org.nuxeo.ecm.core.blob.jit.test:OSGI-INF/test-stream-contrib.xml")
public class TestStreamStatementProducer {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void canCreateTimeHierarchyMessages() throws Exception{

    	try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Serializable> params = new HashMap<>();

            params.put("nbMonths", 48);
            params.put("logConfig", "chronicle");

            automationService.run(ctx,StatementFolderProducers.ID, params);
                                    
    		LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");
    		
            LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);            

            int count=0;
            LogRecord<DocumentMessage> record=null;
            do {
            	record = tailer.read(Duration.ofSeconds(1));
            	if (record!=null) {
            		DocumentMessage docMessage = record.message();
            		assertEquals("Folder",docMessage.getType());
            		assertTrue(((String)docMessage.getProperties().get("dc:description")).startsWith(StatementFolderMessageProducer.FOLDER_DESC_PREFIX));            		
            		count++;
            	}            		
            } while (record!=null);
            
            assertEquals(48+4, count);
            tailer.commit();
            tailer.close();
        }
    }

    protected static String[] expectedAccountID=new String[] {
    		"0D377-07C9D-5D0B414-05",
    		"27180-02DF4-6252532-00",
    		"13A20-07436-34D27B8-05",
    		"19E08-1312B-2CEE0CF-00",
    		"25F4E-00805-422666B-01",
    		"1ABEE-1F712-3BA857E-03",
    		"34AFF-05187-670C565-05",
    		"2CD1E-05D13-706F03E-07",
    		"31BB0-15D7D-0E6F5CA-06",
    		"24601-17E63-6D0825B-04"    };
        
    @Test
    public void canCreateStatementsMessages() throws Exception{

    	try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Serializable> params = new HashMap<>();

            long nbDocs = 48*10;
            
            params.put("nbDocuments", nbDocs);
            params.put("nbMonths", 48);
            params.put("logConfig", "chronicle");
            params.put("nbThreads", 1);
            params.put("seed", AccountHelper.DEFAULT_SEED);

            automationService.run(ctx,StatementProducers.ID, params);
                                    
    		LogManager manager = Framework.getService(StreamService.class).getLogManager("chronicle");
    		    		
            LogTailer<DocumentMessage> tailer = manager.createTailer("test", StreamImporters.DEFAULT_LOG_DOC_NAME);            

            int count=0;
            int idx=0;
            String lastAccounId=null;
            LogRecord<DocumentMessage> record=null;
            do {
            	record = tailer.read(Duration.ofSeconds(1));
            	if (record!=null) {
            		DocumentMessage docMessage = record.message();
            		assertEquals("Statement",docMessage.getType());
            		assertEquals("initialImport", docMessage.getProperties().get("dc:source"));            		
            		
            		String account = (String) docMessage.getProperties().get("statement:accountNumber");
            		String customer = (String) docMessage.getProperties().get("all:customerNumber");
            		
            		if (count%48==0) {
            			lastAccounId = account;
            			assertEquals(expectedAccountID[idx],lastAccounId);
            			assertTrue(expectedAccountID[idx].contains(customer));
            			idx++;
            		} else {
            			assertEquals(lastAccounId, account);
            		}            		
            		count++;
            	}            		
            } while (record!=null);
            
            assertEquals(nbDocs, count);
            tailer.commit();
            tailer.close();
        }
    	    	
    	    	
    }
    
    @Test
    public void canRegenerateTheSameAccounts() throws Exception {		
    	for (int i = 0; i < 10; i++) {
    		String id = AccountHelper.instance().getNextId();
    		assertEquals(expectedAccountID[i], id);
    	}

    }
}
