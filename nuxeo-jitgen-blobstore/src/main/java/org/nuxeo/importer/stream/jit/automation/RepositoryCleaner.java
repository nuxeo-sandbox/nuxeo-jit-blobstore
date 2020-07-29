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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.client.MongoDatabase;

@Operation(id = RepositoryCleaner.ID, category = Constants.CAT_SERVICES, label = "Wipe the whole repository", since = "11.1", description = "")
public class RepositoryCleaner {
	private static final Log log = LogFactory.getLog(RepositoryCleaner.class);

	public static final String ID = "Benchmark.cleanRepository";

	@Context
	protected OperationContext context;

	@Param(name = "repositoryName", required = true)
	protected String repositoryName = null;

	@Param(name = "confirmationKey", required = false)
	protected String confirmationKey = null;

	protected static String key = null;

	protected void checkAccess() {
		NuxeoPrincipal principal = context.getPrincipal();
		if (principal == null || !principal.isAdministrator()) {
			throw new RuntimeException("Unauthorized access: " + principal);
		}
	}

	@OperationMethod
	public String run() throws OperationException {

		checkAccess();

		// simple safety precaution
		// to avoid deleting the repository after a bad copy paste

		// if no key provided, generate one
		if (confirmationKey == null) {
			key = UUID.randomUUID().toString();
			return key;
		}
		// if the key is not good return the correct one
		if (!confirmationKey.equals(key)) {
			return key;
		}
		// if the key was provided and good: burn it!
		key = null;

		
		List<String> repos = new ArrayList<>();
		
		if ("*".equals(repositoryName)) {
			RepositoryManager rm = Framework.getService(RepositoryManager.class);
			repos.addAll(rm.getRepositoryNames());
		} else {
			repos.add(repositoryName);
		}
		
		
		for (String repo: repos) {
			resetMongoDB(repo);
			resetES(repo);		
		}		
	
		return "Cleaned up MongoDB and ES for " + String.join(" ", repos) + ".";
	}

	public void resetMongoDB(String repositoryName) {
		MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
		MongoDatabase database = mongoService.getDatabase("repository/" + repositoryName);
		database.getCollection(repositoryName).drop();
		database.getCollection(repositoryName + ".counters").drop();
	}

	public void resetES(String repositoryName) {
		ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
		esa.dropAndInitRepositoryIndex(repositoryName);
		esa.flushRepositoryIndex(repositoryName);
	}

}