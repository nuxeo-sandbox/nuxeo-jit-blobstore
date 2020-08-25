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

package org.nuxeo.data.gen.cli.tests;


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
import org.nuxeo.importer.stream.jit.AccountMessageProducerFactory;
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

@Operation(id = NOPOperation.ID, category = Constants.CAT_SERVICES)
public class NOPOperation {
    private static final Log log = LogFactory.getLog(NOPOperation.class);

    public static final String ID = "Test.doNothing";

    @Context
    protected OperationContext context;

    @Param(name = "duration", required = false)
    protected Integer duration;

    @OperationMethod
    public void run() throws OperationException {
       
    	if (duration==null || duration==0) {
    		duration=10;
    	}
    	
    	try {
			Thread.sleep(1000*duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    }


}
