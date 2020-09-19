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

package org.nuxeo.ecm.core.blob.jit.es;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.BROWSE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.UNSUPPORTED_ACL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.blob.jit.thumbnail.StatementThumbnailFactory;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.elasticsearch.io.JsonESDocumentWriter;
import org.nuxeo.importer.stream.jit.USStateHelper;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

public class StatementESDocumentWriter extends JsonESDocumentWriter {
	
    private static final Log log = LogFactory.getLog(StatementESDocumentWriter.class);
    
    private final HashSet<String> browsePermissions;
    
    public StatementESDocumentWriter() {
    	SecurityService securityService = Framework.getService(SecurityService.class);
		browsePermissions = new HashSet<>(Arrays.asList(securityService.getPermissionsToCheck(BROWSE)));		
    }
    
    protected void writeSchemas(JsonGenerator jg, DocumentModel doc, String[] schemas) throws IOException {
    	if (limitedIndexing(doc)) {
    		 // only index some schemas
    		 writeProperties(jg, doc, "account", null);
    		 writeProperties(jg, doc, "all", null); 		 
    	} else {
    		super.writeSchemas(jg, doc, schemas);
    	}
    }
        
	protected void writeSystemProperties(JsonGenerator jg, DocumentModel doc) throws IOException {
		String docId = doc.getId();
		
		jg.writeStringField("ecm:repository", doc.getRepositoryName());
		jg.writeStringField("ecm:uuid", docId);
		jg.writeStringField("ecm:name", doc.getName());
		jg.writeStringField("ecm:title", doc.getTitle());

		String pathAsString = doc.getPathAsString();
		jg.writeStringField("ecm:path", pathAsString);
/*		if (StringUtils.isNotBlank(pathAsString)) {
			String[] split = pathAsString.split("/");
			if (split.length > 0) {
				for (int i = 1; i < split.length; i++) {
					jg.writeStringField("ecm:path@level" + i, split[i]);
				}
			}
			jg.writeNumberField("ecm:path@depth", split.length);
		}*/

		jg.writeStringField("ecm:primaryType", doc.getType());
		DocumentRef parentRef = doc.getParentRef();
		if (parentRef != null) {
			jg.writeStringField("ecm:parentId", parentRef.toString());
		}
		jg.writeStringField("ecm:currentLifeCycleState", doc.getCurrentLifeCycleState());
		if (doc.isVersion()) {
			jg.writeStringField("ecm:versionLabel", doc.getVersionLabel());
			jg.writeStringField("ecm:versionVersionableId", doc.getVersionSeriesId());
		}
		jg.writeBooleanField("ecm:isCheckedIn", !doc.isCheckedOut());
		jg.writeBooleanField("ecm:isProxy", doc.isProxy());
		jg.writeBooleanField("ecm:isTrashed", doc.isTrashed());
		jg.writeBooleanField("ecm:isVersion", doc.isVersion());
		jg.writeBooleanField("ecm:isLatestVersion", doc.isLatestVersion());
		jg.writeBooleanField("ecm:isLatestMajorVersion", doc.isLatestMajorVersion());
		jg.writeBooleanField("ecm:isRecord", doc.isRecord());
		Calendar retainUntil = doc.getRetainUntil();
		if (retainUntil != null) {
			jg.writeStringField("ecm:retainUntil", retainUntil.toInstant().toString());
		}
		jg.writeBooleanField("ecm:hasLegalHold", doc.hasLegalHold());
		jg.writeArrayFieldStart("ecm:mixinType");
		for (String facet : doc.getFacets()) {
			jg.writeString(facet);
		}
		jg.writeEndArray();
		if (!limitedIndexing(doc)) {
			CoreSession session = doc.getCoreSession();
			TagService tagService = Framework.getService(TagService.class);
			if (tagService != null && tagService.supportsTag(session, docId)) {
				jg.writeArrayFieldStart("ecm:tag");
				for (String tag : tagService.getTags(session, docId)) {
					jg.writeString(tag);
				}
				jg.writeEndArray();
			}
		}
		jg.writeStringField("ecm:changeToken", doc.getChangeToken());
		Long pos = doc.getPos();
		if (pos != null) {
			jg.writeNumberField("ecm:pos", pos.longValue());
		}
		// Add a positive ACL only
		ACP acp = doc.getACP();
		if (acp == null) {
			acp = new ACPImpl();
		}
		jg.writeArrayFieldStart("ecm:acl");
		outerloop: for (ACL acl : acp.getACLs()) {
			for (ACE ace : acl.getACEs()) {
				if (ace.isGranted() && ace.isEffective() && browsePermissions.contains(ace.getPermission())) {
					jg.writeString(ace.getUsername());
				}
				if (ace.isDenied() && ace.isEffective()) {
					if (!EVERYONE.equals(ace.getUsername())) {
						jg.writeString(UNSUPPORTED_ACL);
					}
					break outerloop;
				}
			}
		}

		jg.writeEndArray();
		if (!limitedIndexing(doc)) {
			Map<String, String> bmap = getFullText(doc);
			if (bmap != null && !bmap.isEmpty()) {
				for (Map.Entry<String, String> item : bmap.entrySet()) {
					String value = item.getValue();
					if (value != null) {
						jg.writeStringField("ecm:" + item.getKey(), value);
					}
				}
			}
		}
	}

	public Map<String, String> getFullText(DocumentModel doc) throws IOException {
		if (doc.getType().equalsIgnoreCase("Statement")) {
			String txtContent="";
			if (!limitedIndexing(doc)) {
				Blob smt = (Blob) doc.getPropertyValue("file:content");
				try {
					txtContent = BlobTextExtractor.instance().getTextFromBlob(smt);
				} catch (Exception e) {
					log.error("Unable to extract text for Statement with UID" + doc.getId(), e);
				}
			}
			return Collections.singletonMap("binarytext", txtContent);
		} else if (doc.getType().equals("IDCard") || doc.getType().equals("Customer")) {
			String txt = (String) doc.getPropertyValue("customer:firstname");
			txt = txt + " " + (String) doc.getPropertyValue("customer:lastname");		
			txt = txt + " " + (String) doc.getPropertyValue("customer:number");		
			return Collections.singletonMap("binarytext", txt);			
		} else if (doc.getType().equals("Account") || doc.getType().startsWith("Correspondence")) {
			String txt = (String) doc.getPropertyValue("customer:firstname");
			txt = txt + " " + (String) doc.getPropertyValue("customer:lastname");		
			txt = txt + " " + (String) doc.getPropertyValue("customer:number");		
			txt = txt + " " + (String) doc.getPropertyValue("account:number");		
			return Collections.singletonMap("binarytext", txt);			
		}
		else {
			return doc.getBinaryFulltext();
		}
	}
	
	protected boolean limitedIndexing(DocumentModel doc) {		
		return "archives".equalsIgnoreCase(doc.getRepositoryName()) && "Statement".equalsIgnoreCase(doc.getType());
	}
}
