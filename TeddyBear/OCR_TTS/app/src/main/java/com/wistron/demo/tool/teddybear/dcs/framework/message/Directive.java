package com.wistron.demo.tool.teddybear.dcs.framework.message;

import com.wistron.demo.tool.teddybear.dcs.util.ObjectMapperUtil;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ivanjlzhang on 17-9-21.
 */
@JsonDeserialize(using = Directive.DirectiveDeserializer.class)
public class Directive {
    public Header header;
    public Payload payload;

    @JsonIgnore
    public String rawMessage;


    public Directive(Header header, JsonNode payload, String rawMessage) throws IOException {
        this.header = header;
        String namespace = header.getNamespace();
        String name = header.getName();
        Class<?> payloadType = PayloadConfig.getInstance().findPayloadClass(namespace, name);
        if (null != payloadType) {
            this.payload = ObjectMapperUtil.instance().getObjectReader().withType(payloadType).readValue(payload);
        } else {
            this.payload = new Payload();
        }

        this.rawMessage = rawMessage;
    }

    /**
     * jackson反序列化directive对象
     */
    public static class DirectiveDeserializer extends JsonDeserializer<Directive> {
        @Override
        public Directive deserialize(JsonParser jp, DeserializationContext ctx)
                throws IOException {
            ObjectReader reader = ObjectMapperUtil.instance().getObjectReader();
            ObjectNode obj = (ObjectNode) reader.readTree(jp);
            Iterator<Map.Entry<String, JsonNode>> elementsIterator = obj.getFields();

            String rawMessage = obj.toString();
            DialogRequestIdHeader header = null;
            JsonNode payloadNode = null;
            ObjectReader headerReader =
                    ObjectMapperUtil.instance().getObjectReader(DialogRequestIdHeader.class);
            while (elementsIterator.hasNext()) {
                Map.Entry<String, JsonNode> element = elementsIterator.next();
                if (element.getKey().equals("header")) {
                    header = headerReader.readValue(element.getValue());
                }
                if (element.getKey().equals("payload")) {
                    payloadNode = element.getValue();
                }
            }
            if (header == null) {
                throw ctx.mappingException("Missing header");
            }
            if (payloadNode == null) {
                throw ctx.mappingException("Missing payload");
            }

            return createDirective(header, payloadNode, rawMessage);
        }

        private Directive createDirective(Header header, JsonNode payload, String rawMessage)
                throws JsonParseException, JsonMappingException, IOException {
            return new Directive(header, payload, rawMessage);
        }
    }

    @JsonIgnore
    public String getName() {
        return header.getName();
    }

    public Payload getPayload() {
        return payload;
    }
}
