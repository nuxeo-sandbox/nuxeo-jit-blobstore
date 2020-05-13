/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.OVERRIDE_REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.restapi.server.jaxrs.enrichers.HasContentJsonEnricher;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

@Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
public class DummyHasContentJsonEnricher extends HasContentJsonEnricher {

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        if (document.isFolder()) {
            jg.writeBooleanField(NAME, true);
        } else if (Framework.getService(CollectionManager.class).isCollection(document)) {
            jg.writeBooleanField(NAME, true);
        } else {
            jg.writeBooleanField(NAME, false);
        }
    }
}
