/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.OVERRIDE_REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter;

import com.fasterxml.jackson.core.JsonGenerator;

@Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
public class DummyNuxeoPrincipalJsonWriter extends NuxeoPrincipalJsonWriter {

	public static final String ENTITY_TYPE = "user";

	public DummyNuxeoPrincipalJsonWriter() {
		super();
	}

	@Override
	protected void writeEntityBody(NuxeoPrincipal principal, JsonGenerator jg) throws IOException {
		jg.writeStringField("id", principal.getName());
		// writeProperties(jg, principal);
		// writeExtendedGroups(jg, principal);
		jg.writeBooleanField("isAdministrator", principal.isAdministrator());
		jg.writeBooleanField("isAnonymous", principal.isAnonymous());
	}

}
