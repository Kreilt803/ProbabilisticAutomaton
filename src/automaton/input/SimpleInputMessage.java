package automaton.input;

import java.util.Collections;
import java.util.Map;

/**
 * Простая реализация InputMessage с сырой строкой и опциональной картой атрибутов.
 */
public class SimpleInputMessage implements InputMessage {

    private final String raw;
    private final Map<String, Object> attributes;

    public SimpleInputMessage(String raw, Map<String, Object> attributes) {
        this.raw = raw;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    public static SimpleInputMessage of(String raw) {
        return new SimpleInputMessage(raw, Collections.emptyMap());
    }

    @Override
    public String getRaw() {
        return raw;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
