package org.nuxeo.ecm.core.io.marshallers.json.document;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher.ENTITY_ENRICHER_NAME;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.OVERRIDE_REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.Enriched;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.MaxDepthReachedException;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.WrappedContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;


/**
 * @since 11.1
 */
@Setup(mode = SINGLETON, priority = OVERRIDE_REFERENCE)
public class TracingDocumentModelJsonWriter extends DocumentModelJsonWriter {

    protected Type genericType = TypeUtils.parameterize(Enriched.class, DocumentModel.class);

    @Override
    public void write(DocumentModel entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        List<Object> entityList = new ArrayList<>();
        entityList.add(ENTITY_TYPE);
        ctx.addParameterListValues(RenderingContext.RESPONSE_HEADER_ENTITY_TYPE_KEY, entityList);
        jg.writeStringField(ENTITY_FIELD_NAME, ENTITY_TYPE);
        writeEntityBody(entity, jg);
        try {
            WrappedContext wrappedCtx = ctx.wrap().controlDepth();
            Set<String> enrichers = ctx.getEnrichers(ENTITY_TYPE);
            if (enrichers.size() > 0) {
                Map<String, Long> traces = new HashMap<>();
                boolean hasEnrichers = false;
                Enriched<DocumentModel> enriched = null;
                for (String enricherName : enrichers) {
                    try (Closeable resource = wrappedCtx.with(ENTITY_ENRICHER_NAME, enricherName).open()) {
                        Collection<Writer<Enriched>> writers = registry.getAllWriters(ctx, Enriched.class,
                                this.genericType, APPLICATION_JSON_TYPE);
                        for (Writer<Enriched> writer : writers) {
                            if (!hasEnrichers) {
                                hasEnrichers = true;
                                jg.writeObjectFieldStart("contextParameters");
                                enriched = new Enriched<>(entity);
                            }
                            OutputStreamWithJsonWriter out = new OutputStreamWithJsonWriter(jg);
                            long startTime = System.nanoTime();
                            writer.write(enriched, Enriched.class, this.genericType, APPLICATION_JSON_TYPE, out);
                            traces.put(enricherName, System.nanoTime() - startTime);
                        }
                    }
                }
                // XXX - added tracing
                jg.writeObjectField("tracing", traces);
                if (hasEnrichers) {
                    jg.writeEndObject();
                }
            }
        } catch (MaxDepthReachedException e) {
            // do nothing, do not call enrichers
        }
        extend(entity, jg);
        jg.writeEndObject();
    }

}