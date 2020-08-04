
package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.OVERRIDE_REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

@Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
public class RenditionJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

	public static final String RENDITION_REST_URL_FORMAT = "%sapi/v1/repo/%s/id/%s/@rendition/%s";

	public static final String NAME = "renditions";

	public RenditionJsonEnricher() {
		super(NAME);
	}

	@Override
	public void write(JsonGenerator jg, DocumentModel document) throws IOException {
		RenditionService renditionService = Framework.getService(RenditionService.class);
		List<Rendition> renditions = renditionService.getAvailableRenditions(document, true);
		jg.writeArrayFieldStart(NAME);
		for (Rendition rendition : renditions) {
			jg.writeStartObject();
			jg.writeStringField("name", rendition.getName());
			jg.writeStringField("kind", rendition.getKind());
			jg.writeStringField("icon", ctx.getBaseUrl().replaceAll("/$", "") + rendition.getIcon());
			jg.writeStringField("url", String.format(RENDITION_REST_URL_FORMAT, ctx.getBaseUrl(),
					document.getRepositoryName(), document.getId(), rendition.getName()));
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}

}
