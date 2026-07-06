package uk.gov.companieshouse.registers.util;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public class EmptyFieldDeserializer  extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext context) {
        JsonNode node = jsonParser.readValueAsTree();
        String str = node.asString();
        if (str.isEmpty()) {
            return null;
        }
        return str;
    }
}
