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

package org.nuxeo.importer.stream.jit.automation;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.jit.CustomerMessageProducerFactory;
import org.nuxeo.importer.stream.jit.CustomerMessageProducerFactoryMT;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.importer.stream.producer.MultiRepositoriesProducerPool;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

@Operation(id = CustomerProducersMT.ID, category = Constants.CAT_SERVICES, label = "Produces Bank Statements", since = "11.1", description = "")
public class CustomerProducersMT {
    private static final Log log = LogFactory.getLog(CustomerProducersMT.class);

    public static final String ID = "StreamImporter.runConsumerProducersMT";

    @Context
    protected OperationContext context;

    @Param(name = "logName", required = false)
    protected String logName = StreamImporters.DEFAULT_LOG_DOC_NAME;

    @Param(name = "logSize", required = false)
    protected Integer logSize;

    @Param(name = "split", required = false)
    protected Boolean splitOutput = false;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads = 8;

    protected static final int BUFFER_SIZE=1000;    
    
    protected void checkAccess() {
        NuxeoPrincipal principal = context.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }

    @OperationMethod
    public void run(Blob csvData) throws OperationException {
        
    	checkAccess();
    	    	
    	LogManager manager = Framework.getService(StreamService.class).getLogManager();        
        if (splitOutput) {
        	manager.createIfNotExists(MultiRepositoriesProducerPool.getLogName(logName, USStateHelper.EAST), getLogSize());
        	manager.createIfNotExists(MultiRepositoriesProducerPool.getLogName(logName, USStateHelper.WEST), getLogSize());      	
        } else {
        	manager.createIfNotExists(Name.ofUrn(logName), getLogSize());            	
        }
           	
    	BufferedReader reader;
    	List<List<String>> csvChunks = new ArrayList<>();
    	for (int i = 0; i < nbThreads;i++) {
    		csvChunks.add(new ArrayList<String>());
    	}
    	
    	try {
			reader = new BufferedReader(new InputStreamReader(csvData.getStream()));
			int idx=0;
	    	while(reader.ready()) {
	    		csvChunks.get(idx%nbThreads).add(reader.readLine());
	    	    idx++;
	    	}	    	
    		startProducer(manager, csvChunks, nbThreads);
    	} catch (IOException e) {
			throw new OperationException("Unable to read input CSV", e);
		}
       
    }

    protected void startProducer(LogManager manager, List<List<String>> csvChunks, int nbThreads) throws OperationException {

   
    	CustomerMessageProducerFactoryMT factory = new CustomerMessageProducerFactoryMT(csvChunks);
    	Codec<DocumentMessage> codec = StreamImporters.getDocCodec();
    	
        try (ProducerPool<DocumentMessage> producers = new MultiRepositoriesProducerPool<>(logName, manager, codec, factory,
        		(short) nbThreads, (boolean) splitOutput)) {
            producers.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Operation interrupted");
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("Operation fails", e);
            throw new OperationException(e);
        }    
    }
    
    protected int getLogSize() {
        if (logSize != null && logSize > 0) {
            return logSize;
        }
        return 1;
    }

}
