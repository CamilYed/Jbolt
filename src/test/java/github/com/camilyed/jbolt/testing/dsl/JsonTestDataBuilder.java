package github.com.camilyed.jbolt.testing.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Advanced JSON builder for test data orchestration.
 */
public class JsonTestDataBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ObjectNode root = mapper.createObjectNode();

    public static JsonTestDataBuilder aJson() {
        return new JsonTestDataBuilder();
    }

    public JsonTestDataBuilder withField(String key, String value) {
        root.put(key, value);
        return this;
    }

    public JsonTestDataBuilder withField(String key, boolean value) {
        root.put(key, value);
        return this;
    }

    public JsonTestDataBuilder withField(String key, int value) {
        root.put(key, value);
        return this;
    }

    public JsonTestDataBuilder withArray(String key, Object... values) {
        ArrayNode array = root.putArray(key);
        for (Object v : values) {
            if (v instanceof String s) array.add(s);
            else if (v instanceof Integer i) array.add(i);
        }
        return this;
    }

    public JsonTestDataBuilder withMap(String key, Map<String, Object> map) {
        ObjectNode mapNode = root.putObject(key);
        map.forEach((k, v) -> {
            if (v instanceof String s) mapNode.put(k, s);
            else if (v instanceof Integer i) mapNode.put(k, i);
        });
        return this;
    }

    @Override
    public String toString() {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}