package org.nuxeo.importer.stream.jit;

import java.util.EnumSet;

import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.Record.Flag;
import org.nuxeo.lib.stream.pattern.Message;

public class RecordDocumentMessage extends Record implements Message {
		
	public RecordDocumentMessage() {
		super();
	}
		
    public RecordDocumentMessage(DocumentMessage docMessage) {
    	Codec<DocumentMessage> codec = new SerializableCodec<DocumentMessage>();
        setKey(docMessage.getId());
        setWatermark(Watermark.ofNow().getValue());
        setData(codec.encode(docMessage));
        setFlags(EnumSet.of(Flag.DEFAULT));
    }

	
	@Override
	public String getId() {
		return getKey();
	}

	public DocumentMessage getDocumentMessage() {
		Codec<DocumentMessage> codec = new SerializableCodec<DocumentMessage>();		
		return codec.decode(getData());
	}
	
}
