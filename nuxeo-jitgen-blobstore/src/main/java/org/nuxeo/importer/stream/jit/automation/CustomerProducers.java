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
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.importer.stream.producer.MultiRepositoriesProducerPool;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

@Operation(id = CustomerProducers.ID, category = Constants.CAT_SERVICES, label = "Produces Bank Statements", since = "11.1", description = "")
public class CustomerProducers {
    private static final Log log = LogFactory.getLog(CustomerProducers.class);

    public static final String ID = "StreamImporter.runConsumerProducers";

    @Context
    protected OperationContext context;

    @Param(name = "logName", required = false)
    protected String logName = StreamImporters.DEFAULT_LOG_DOC_NAME;

    @Param(name = "logSize", required = false)
    protected Integer logSize;

    @Param(name = "bufferSize", required = false)
    protected Integer bufferSize = BUFFER_SIZE;

    @Param(name = "split", required = false)
    protected Boolean splitOutput = false;

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
    	try {
			reader = new BufferedReader(new InputStreamReader(csvData.getStream()));
			String[] lines = new String[bufferSize];
			int idx=0;
	    	while(reader.ready()) {
	    		lines[idx] = reader.readLine();
	    	    idx++;
	    	    if (idx>= lines.length) {
	    	    	startProducer(manager, lines);
	    	    	lines = new String[bufferSize];
	    	    	idx=0;
	    	    }	    	     
	    	}
	    	
	    	if (idx>0) {
	    		startProducer(manager, lines);
	    	}

    	} catch (IOException e) {
			throw new OperationException("Unable to read input CSV", e);
		}
       
    }

    protected void startProducer(LogManager manager, String[] lines) throws OperationException {

    	CustomerMessageProducerFactory factory = new CustomerMessageProducerFactory(lines);
    	Codec<DocumentMessage> codec = StreamImporters.getDocCodec();
    	
        try (ProducerPool<DocumentMessage> producers = new MultiRepositoriesProducerPool<>(logName, manager, codec, factory,
        		(short) 1, (boolean) splitOutput)) {
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
