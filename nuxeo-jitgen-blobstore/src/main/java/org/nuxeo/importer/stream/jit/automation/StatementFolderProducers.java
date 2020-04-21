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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.importer.stream.jit.StatementFolderMessageProducerFactory;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

@Operation(id = StatementFolderProducers.ID, category = Constants.CAT_SERVICES, label = "Produces Folders for Bank Statements", since = "11.1", description = "")
public class StatementFolderProducers {
	private static final Log log = LogFactory.getLog(StatementFolderProducers.class);

	public static final String ID = "StreamImporter.runStatementFolderProducers";

	@Context
	protected OperationContext context;

	@Param(name = "nbMonths")
	protected Integer nbMonths;

	@Param(name = "logName", required = false)
	protected String logName = StreamImporters.DEFAULT_LOG_DOC_NAME;

	@Param(name = "logSize", required = false)
	protected Integer logSize;

	@Param(name = "logConfig", required = false)
	protected String logConfig = StreamImporters.DEFAULT_LOG_CONFIG;

	protected void checkAccess() {
		NuxeoPrincipal principal = context.getPrincipal();
		if (principal == null || !principal.isAdministrator()) {
			throw new RuntimeException("Unauthorized access: " + principal);
		}
	}

	@OperationMethod
	public void run() throws OperationException {

		checkAccess();

		LogManager manager = Framework.getService(StreamService.class).getLogManager(logConfig);
		manager.createIfNotExists(logName, getLogSize());

		StatementFolderMessageProducerFactory factory = new StatementFolderMessageProducerFactory(nbMonths);


		try (ProducerPool<DocumentMessage> producers = new ProducerPool<>(logName, manager, factory,
				(short) 1)) {
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
